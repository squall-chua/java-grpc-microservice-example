package com.example.demo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.auth.v1.AuthOptionsProto;
import com.example.auth.v1.AuthRule;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.grpc.protobuf.ProtoMethodDescriptorSupplier;

@ExtendWith(MockitoExtension.class)
public class GrpcSecurityInterceptorTest {

    private GrpcSecurityInterceptor interceptor;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter mockCounter;

    @Mock
    private ServerCall<Object, Object> serverCall;

    @Mock
    private ServerCallHandler<Object, Object> nextHandler;

    @Mock
    private io.grpc.MethodDescriptor<Object, Object> grpcMethodDescriptor;

    @Mock
    private ProtoMethodDescriptorSupplier methodDescriptorSupplier;

    @Mock
    private Descriptors.MethodDescriptor protoMethodDescriptor;

    @Mock
    private DescriptorProtos.MethodOptions methodOptions;

    @Captor
    private ArgumentCaptor<Status> statusCaptor;

    @Captor
    private ArgumentCaptor<Metadata> metadataCaptor;

    private Metadata headers;

    @BeforeEach
    void setUp() {
        interceptor = new GrpcSecurityInterceptor("secret", meterRegistry);
        ReflectionTestUtils.setField(interceptor, "jwtDecoder", jwtDecoder);
        headers = new Metadata();
        lenient().when(meterRegistry.counter(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockCounter);
    }

    private void setupMockMethodDescriptor(boolean hasRule, AuthRule rule) {
        when(serverCall.getMethodDescriptor()).thenReturn(grpcMethodDescriptor);
        when(grpcMethodDescriptor.getSchemaDescriptor()).thenReturn(methodDescriptorSupplier);
        when(methodDescriptorSupplier.getMethodDescriptor()).thenReturn(protoMethodDescriptor);
        when(protoMethodDescriptor.getOptions()).thenReturn(methodOptions);
        when(methodOptions.hasExtension(AuthOptionsProto.rule)).thenReturn(hasRule);
        if (hasRule && rule != null) {
            when(methodOptions.getExtension(AuthOptionsProto.rule)).thenReturn(rule);
        }
    }

    @Test
    void testMethodWithoutSecurityRules_PassesThrough() {
        // Arrange
        when(serverCall.getMethodDescriptor()).thenReturn(grpcMethodDescriptor);
        when(grpcMethodDescriptor.getSchemaDescriptor()).thenReturn(methodDescriptorSupplier);
        when(methodDescriptorSupplier.getMethodDescriptor()).thenReturn(protoMethodDescriptor);
        when(protoMethodDescriptor.getOptions()).thenReturn(methodOptions);
        when(methodOptions.hasExtension(AuthOptionsProto.rule)).thenReturn(false);

        // Act
        interceptor.interceptCall(serverCall, headers, nextHandler);

        // Assert
        verify(nextHandler, times(1)).startCall(serverCall, headers);
        verify(serverCall, never()).close(any(Status.class), any(Metadata.class));
    }

    @Test
    void testMethodWithSecurityRules_NoAuthHeader_ReturnsUnauthenticated() {
        // Arrange
        AuthRule rule = AuthRule.newBuilder().build();
        setupMockMethodDescriptor(true, rule);

        // Act
        interceptor.interceptCall(serverCall, headers, nextHandler);

        // Assert
        verify(serverCall).close(statusCaptor.capture(), metadataCaptor.capture());
        assertEquals(Status.Code.UNAUTHENTICATED, statusCaptor.getValue().getCode());
        verify(nextHandler, never()).startCall(any(), any());
    }

    @Test
    void testMethodWithSecurityRules_InvalidAuthHeaderFormat_ReturnsUnauthenticated() {
        // Arrange
        AuthRule rule = AuthRule.newBuilder().build();
        setupMockMethodDescriptor(true, rule);
        headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "InvalidFormat");

        // Act
        interceptor.interceptCall(serverCall, headers, nextHandler);

        // Assert
        verify(serverCall).close(statusCaptor.capture(), metadataCaptor.capture());
        assertEquals(Status.Code.UNAUTHENTICATED, statusCaptor.getValue().getCode());
    }

    @Test
    void testMethodWithSecurityRules_InvalidJwtToken_ReturnsUnauthenticated() {
        // Arrange
        AuthRule rule = AuthRule.newBuilder().build();
        setupMockMethodDescriptor(true, rule);
        headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer invalid_token");
        when(jwtDecoder.decode("invalid_token")).thenThrow(new RuntimeException("Invalid token"));

        // Act
        interceptor.interceptCall(serverCall, headers, nextHandler);

        // Assert
        verify(serverCall).close(statusCaptor.capture(), metadataCaptor.capture());
        assertEquals(Status.Code.UNAUTHENTICATED, statusCaptor.getValue().getCode());
    }

    @Test
    void testMethodRequiresScopes_TokenMissingScope_ReturnsPermissionDenied() {
        // Arrange
        AuthRule rule = AuthRule.newBuilder().addRequiredScopes("admin").build();
        setupMockMethodDescriptor(true, rule);
        headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer valid_token");

        Jwt jwt = mock(Jwt.class);
        when(jwtDecoder.decode("valid_token")).thenReturn(jwt);
        when(jwt.getClaimAsString("scopes")).thenReturn("user guest");

        // Act
        interceptor.interceptCall(serverCall, headers, nextHandler);

        // Assert
        verify(serverCall).close(statusCaptor.capture(), metadataCaptor.capture());
        assertEquals(Status.Code.PERMISSION_DENIED, statusCaptor.getValue().getCode());
    }

    @Test
    void testMethodRequiresScopes_TokenHasScope_PassesThrough() {
        // Arrange
        AuthRule rule = AuthRule.newBuilder().addRequiredScopes("admin").build();
        setupMockMethodDescriptor(true, rule);
        headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer valid_token");

        Jwt jwt = mock(Jwt.class);
        when(jwtDecoder.decode("valid_token")).thenReturn(jwt);
        when(jwt.getClaimAsString("scopes")).thenReturn("user admin guest");
        when(jwt.getSubject()).thenReturn("test-user");
        when(serverCall.getMethodDescriptor()).thenReturn(grpcMethodDescriptor);
        when(grpcMethodDescriptor.getFullMethodName()).thenReturn("example.Service/Method");

        // Act
        interceptor.interceptCall(serverCall, headers, nextHandler);

        // Assert
        verify(nextHandler, times(1)).startCall(serverCall, headers);
    }

    @Test
    void testMethodRequiresRoles_TokenMissingRole_ReturnsPermissionDenied() {
        // Arrange
        AuthRule rule = AuthRule.newBuilder().addRequiredRoles("SUPER_ADMIN").build();
        setupMockMethodDescriptor(true, rule);
        headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer valid_token");

        Jwt jwt = mock(Jwt.class);
        when(jwtDecoder.decode("valid_token")).thenReturn(jwt);
        when(jwt.getClaimAsString("roles")).thenReturn("USER MODERATOR");

        // Act
        interceptor.interceptCall(serverCall, headers, nextHandler);

        // Assert
        verify(serverCall).close(statusCaptor.capture(), metadataCaptor.capture());
        assertEquals(Status.Code.PERMISSION_DENIED, statusCaptor.getValue().getCode());
    }

    @Test
    void testMethodRequiresRoles_TokenHasRole_PassesThrough() {
        // Arrange
        AuthRule rule = AuthRule.newBuilder().addRequiredRoles("SUPER_ADMIN").build();
        setupMockMethodDescriptor(true, rule);
        headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer valid_token");

        Jwt jwt = mock(Jwt.class);
        when(jwtDecoder.decode("valid_token")).thenReturn(jwt);
        when(jwt.getClaimAsString("roles")).thenReturn("USER SUPER_ADMIN");
        when(jwt.getSubject()).thenReturn("test-user");
        when(serverCall.getMethodDescriptor()).thenReturn(grpcMethodDescriptor);
        when(grpcMethodDescriptor.getFullMethodName()).thenReturn("example.Service/Method");

        // Act
        interceptor.interceptCall(serverCall, headers, nextHandler);

        // Assert
        verify(nextHandler, times(1)).startCall(serverCall, headers);
    }
}
