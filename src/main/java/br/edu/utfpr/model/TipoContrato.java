package br.edu.utfpr.model;

public enum TipoContrato {

    CLT(480),
    PJ(480),
    TEMPORARIO(480),
    TERCEIRIZADO(480),
    ESTAGIARIO(360),
    APRENDIZ(360);

    private final int metaMinutosDiarios;

    TipoContrato(int metaMinutosDiarios) {
        this.metaMinutosDiarios = metaMinutosDiarios;
    }

    public int getMetaMinutosDiarios() {
        return metaMinutosDiarios;
    }

    public static TipoContrato fromString(String valor) {
        if (valor == null || valor.isBlank()) {
            return CLT;
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CLT;
        }
    }
}
