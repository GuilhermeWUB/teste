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
    USER("USER", "Associado", false),
    RH("RH", "RH", true),
    FINANCEIRO("FINANCEIRO", "Financeiro", true),
    COMERCIAL("COMERCIAL", "Comercial", true),
    MARKETING("MARKETING", "Marketing", true),
    EVENTOS("EVENTOS", "Eventos", true),
    RETENCAO("RETENCAO", "Retenção", true),
    DIRETORIA("DIRETORIA", "Diretoria", true),
    GERENTE("GERENTE", "Gerente", true),
    GESTOR("GESTOR", "Gestor", true),
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

    /**
     * Retorna os códigos dos roles que podem gerenciar usuários (criar, editar, deletar)
     * Apenas ADMIN, DIRETORIA, GERENTE e GESTOR têm essa permissão
     */
    public static String[] userManagementRoleCodes() {
        return new String[]{"ADMIN", "DIRETORIA", "GERENTE", "GESTOR"};
    }

    /**
     * Verifica se um role pode gerenciar usuários
     */
    public static boolean canManageUsers(String code) {
        if (code == null) {
            return false;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return "ADMIN".equals(normalized)
            || "DIRETORIA".equals(normalized)
            || "GERENTE".equals(normalized)
            || "GESTOR".equals(normalized);
    }

    /**
     * Verifica se um role pode criar demandas
     * Apenas ADMIN, DIRETORIA, GERENTE e GESTOR podem criar demandas
     * Colaboradores apenas recebem demandas
     */
    public static boolean canCreateDemands(String code) {
        if (code == null) {
            return false;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return "ADMIN".equals(normalized)
            || "DIRETORIA".equals(normalized)
            || "GERENTE".equals(normalized)
            || "GESTOR".equals(normalized);
    }

    /**
     * Verifica se um role é colaborador
     * Colaboradores são todos os cargos, exceto USER (Associado)
     * Colaboradores apenas recebem demandas, não podem criar
     */
    public static boolean isCollaborator(String code) {
        if (code == null) {
            return false;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        // USER (Associado) não é colaborador
        if ("USER".equals(normalized)) {
            return false;
        }
        // ADMIN, DIRETORIA, GERENTE, GESTOR não são considerados colaboradores (têm mais poder)
        if ("ADMIN".equals(normalized) || "DIRETORIA".equals(normalized)
            || "GERENTE".equals(normalized) || "GESTOR".equals(normalized)) {
            return false;
        }
        // Todos os outros são colaboradores
        return true;
    }

    /**
     * Retorna os roles para os quais o role especificado pode enviar demandas
     * Hierarquia:
     * - ADMIN: Pode enviar para todos (exceto USER)
     * - DIRETORIA: Pode enviar para todos (exceto USER)
     * - GERENTE: Pode enviar para todos, exceto DIRETORIA (e USER)
     * - GESTOR: Pode enviar para todos, exceto DIRETORIA e GERENTE (e USER)
     * - Colaboradores: Não podem criar demandas
     */
    public static List<RoleType> getTargetRolesFor(String code) {
        if (code == null) {
            return Collections.emptyList();
        }

        String normalized = code.trim().toUpperCase(Locale.ROOT);

        // ADMIN e DIRETORIA podem enviar para todos (exceto USER e ADMIN)
        if ("ADMIN".equals(normalized) || "DIRETORIA".equals(normalized)) {
            return Arrays.stream(values())
                    .filter(role -> !role.getCode().equals("USER") && !role.getCode().equals("ADMIN"))
                    .collect(Collectors.toList());
        }

        // GERENTE pode enviar para todos, exceto DIRETORIA (e USER, ADMIN)
        if ("GERENTE".equals(normalized)) {
            return Arrays.stream(values())
                    .filter(role -> !role.getCode().equals("USER")
                                 && !role.getCode().equals("ADMIN")
                                 && !role.getCode().equals("DIRETORIA"))
                    .collect(Collectors.toList());
        }

        // GESTOR pode enviar para todos, exceto DIRETORIA e GERENTE (e USER, ADMIN)
        if ("GESTOR".equals(normalized)) {
            return Arrays.stream(values())
                    .filter(role -> !role.getCode().equals("USER")
                                 && !role.getCode().equals("ADMIN")
                                 && !role.getCode().equals("DIRETORIA")
                                 && !role.getCode().equals("GERENTE"))
                    .collect(Collectors.toList());
        }

        // Colaboradores não podem criar demandas
        return Collections.emptyList();
    }

    /**
     * Retorna os códigos dos roles para os quais o role especificado pode enviar demandas
     */
    public static List<String> getTargetRoleCodesFor(String code) {
        return getTargetRolesFor(code).stream()
                .map(RoleType::getCode)
                .collect(Collectors.toList());
    }
}
