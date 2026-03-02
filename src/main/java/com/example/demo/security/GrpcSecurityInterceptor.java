package com.example.demo.security;

import io.micrometer.core.instrument.MeterRegistry;

import io.grpc.*;
import io.grpc.protobuf.ProtoMethodDescriptorSupplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Arrays;

import com.example.auth.v1.AuthOptionsProto;
import com.example.auth.v1.AuthRule;
import com.google.protobuf.Descriptors.MethodDescriptor;

@Slf4j
@Component
public class GrpcSecurityInterceptor implements ServerInterceptor {

    private final JwtDecoder jwtDecoder;
    private final MeterRegistry meterRegistry;
    private static final Metadata.Key<String> AUTH_HEADER = Metadata.Key.of("Authorization",
            Metadata.ASCII_STRING_MARSHALLER);

    public GrpcSecurityInterceptor(@Value("${jwt.secret:super-secret-key}") String secret,
            MeterRegistry meterRegistry) {
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HMACSHA256")).build();
        this.meterRegistry = meterRegistry;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        if (call.getMethodDescriptor().getSchemaDescriptor() instanceof ProtoMethodDescriptorSupplier supplier) {
            MethodDescriptor methodDescriptor = supplier.getMethodDescriptor();

            if (methodDescriptor.getOptions().hasExtension(AuthOptionsProto.rule)) {
                AuthRule rule = methodDescriptor.getOptions().getExtension(AuthOptionsProto.rule);

                String authHeader = headers.get(AUTH_HEADER);
                log.info("===> AUTH HEADER: {}", authHeader);
                // Also check lowercase authorization in case transcoding lowercases it
                if (authHeader == null) {
                    authHeader = headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER));
                }

                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid Authorization metadata"),
                            new Metadata());
                    return new ServerCall.Listener<ReqT>() {
                    };
                }

                String token = authHeader.substring(7);
                try {
                    Jwt jwt = jwtDecoder.decode(token);

                    // Check Scopes
                    List<String> requiredScopes = rule.getRequiredScopesList();
                    if (!requiredScopes.isEmpty()) {
                        String scopeClaim = jwt.getClaimAsString("scopes");
                        if (scopeClaim == null)
                            scopeClaim = jwt.getClaimAsString("scope");
                        if (scopeClaim == null)
                            scopeClaim = "";
                        List<String> tokenScopes = Arrays.asList(scopeClaim.split(" "));

                        for (String reqScope : requiredScopes) {
                            if (!tokenScopes.contains(reqScope)) {
                                call.close(
                                        Status.PERMISSION_DENIED.withDescription("Missing required scope: " + reqScope),
                                        new Metadata());
                                return new ServerCall.Listener<ReqT>() {
                                };
                            }
                        }
                    }

                    // Check Roles
                    List<String> requiredRoles = rule.getRequiredRolesList();
                    if (!requiredRoles.isEmpty()) {
                        String roleClaim = jwt.getClaimAsString("roles");
                        if (roleClaim == null)
                            roleClaim = "";
                        List<String> tokenRoles = Arrays.asList(roleClaim.split(" "));

                        boolean hasRole = false;
                        for (String reqRole : requiredRoles) {
                            if (tokenRoles.contains(reqRole)) {
                                hasRole = true;
                                break;
                            }
                        }
                        if (!hasRole) {
                            call.close(Status.PERMISSION_DENIED.withDescription("Missing required roles"),
                                    new Metadata());
                            return new ServerCall.Listener<ReqT>() {
                            };
                        }
                    }

                    // Log user action metric
                    String username = jwt.getSubject() != null ? jwt.getSubject() : "unknown";
                    meterRegistry.counter("api.user.actions", "user", username, "method",
                            call.getMethodDescriptor().getFullMethodName()).increment();

                } catch (Exception e) {
                    log.error("===> JWT ERROR", e);
                    call.close(Status.UNAUTHENTICATED.withDescription("Invalid JWT: " + e.getMessage()),
                            new Metadata());
                    return new ServerCall.Listener<ReqT>() {
                    };
                }
            }
        }

        return next.startCall(call, headers);
    }
}
