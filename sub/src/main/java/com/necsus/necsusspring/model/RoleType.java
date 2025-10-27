package com.necsus.necsusspring.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum RoleType {
    ADMIN("ADMIN", "Administrador", true),
    USER("USER", "Usuário", false),
    RH("RH", "RH", true),
    FINANCEIRO("FINANCEIRO", "Financeiro", true),
    COMERCIAL("COMERCIAL", "Comercial", true),
    MARKETING("MARKETING", "Marketing", true),
    EVENTOS("EVENTOS", "Eventos", true),
    RETENCAO("RETENCAO", "Retenção", true),
    DIRETORIA("DIRETORIA", "Diretoria", true),
    TI("TI", "TI", true),
    CLOSERS("CLOSERS", "Closers", true),
    EXTERNOS("EXTERNOS", "Externos", true);

    private static final String ROLE_PREFIX = "ROLE_";

    private final String code;
    private final String displayName;
    private final boolean adminPrivileges;

    RoleType(String code, String displayName, boolean adminPrivileges) {
        this.code = code;
        this.displayName = displayName;
        this.adminPrivileges = adminPrivileges;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean hasAdminPrivileges() {
        return adminPrivileges;
    }

    public static Optional<RoleType> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(roleType -> roleType.code.equals(normalized))
                .findFirst();
    }

    public static RoleType fromCodeOrDefault(String code) {
        return fromCode(code).orElse(USER);
    }

    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }

    public static List<RoleType> assignableRoles() {
        return Collections.unmodifiableList(Arrays.asList(values()));
    }

    public static Map<String, String> displayNameByCode() {
        return Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(RoleType::getCode, RoleType::getDisplayName));
    }

    public static Set<String> adminRoleCodeSet() {
        return Arrays.stream(values())
                .filter(RoleType::hasAdminPrivileges)
                .map(RoleType::getCode)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static String[] adminRoleCodes() {
        return Arrays.stream(values())
                .filter(RoleType::hasAdminPrivileges)
                .map(RoleType::getCode)
                .toArray(String[]::new);
    }

    public static String[] allRoleCodes() {
        return Arrays.stream(values())
                .map(RoleType::getCode)
                .toArray(String[]::new);
    }

    public static String displayNameFor(String code) {
        return fromCode(code)
                .map(RoleType::getDisplayName)
                .orElse(code == null ? "" : code);
    }

    public static boolean hasAdminPrivileges(String code) {
        return fromCode(code).map(RoleType::hasAdminPrivileges).orElse(false);
    }

    public static boolean isAdminAuthority(String authority) {
        if (authority == null) {
            return false;
        }
        String normalized = authority.startsWith(ROLE_PREFIX)
                ? authority.substring(ROLE_PREFIX.length())
                : authority;
        return hasAdminPrivileges(normalized);
    }
}
