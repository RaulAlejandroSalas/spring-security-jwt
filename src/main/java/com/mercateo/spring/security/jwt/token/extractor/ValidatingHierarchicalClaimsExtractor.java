package com.mercateo.spring.security.jwt.token.extractor;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.mercateo.spring.security.jwt.security.config.JWTSecurityConfig;
import com.mercateo.spring.security.jwt.token.claim.JWTClaims;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ValidatingHierarchicalClaimsExtractor {

    public static final String WRAPPED_TOKEN_KEY = "jwt";

    private final TokenProcessor tokenProcessor;

    private final TokenVerifier verifier;

    private final InnerClaimsWrapper collector;

    private final ClaimsValidator claimsValidator;

    private final List<String> requiredClaims;

    private final List<String> namespaces;

    private final Option<JWTVerifier> jwtVerifier;

    public ValidatingHierarchicalClaimsExtractor(JWTSecurityConfig config) {
        this.tokenProcessor = new TokenProcessor();
        jwtVerifier = Option.ofOptional(config.jwtVerifier());
        this.verifier = new TokenVerifier(jwtVerifier);
        requiredClaims = List.ofAll(config.getRequiredClaims());
        namespaces = List.ofAll(config.getNamespaces()).append("");
        this.claimsValidator = new ClaimsValidator(requiredClaims);
        this.collector = new InnerClaimsWrapper();

        config.jwtVerifier().ifPresent(v -> log.info("use JWT verifier {}", v));
    }

    public JWTClaims extractClaims(String tokenString) {
        val extractor = new HierarchicalClaimsExtractor(tokenProcessor, verifier, requiredClaims, namespaces);

        val claims = extractor.extractClaims(tokenString);

        if (jwtVerifier.isDefined()) {
            claimsValidator.ensureAtLeastOneVerifiedToken(extractor.getVerifiedTokenCount());
        }
        claimsValidator.ensurePresenceOfRequiredClaims(claims);

        return JWTClaims
            .builder()
            .claims(collector.wrapInnerClaims(claims))
            .verifiedCount(extractor.getVerifiedTokenCount())
            .token(JWT.decode(tokenString))
            .build();
    }

    public boolean hasJWTVerifier() {
        return jwtVerifier.isDefined();
    }

}
