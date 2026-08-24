package com.gestaocompras.util;

public final class CnpjUtil {

    private static final int[] PESOS_DV1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_DV2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjUtil() {
    }

    public static String limpar(String cnpj) {
        return cnpj == null ? null : cnpj.replaceAll("\\D", "");
    }

    public static boolean valido(String cnpj) {
        String digitos = limpar(cnpj);
        if (digitos == null || !digitos.matches("\\d{14}") || digitos.chars().distinct().count() == 1) {
            return false;
        }
        int dv1 = calcularDigito(digitos.substring(0, 12), PESOS_DV1);
        int dv2 = calcularDigito(digitos.substring(0, 13), PESOS_DV2);
        return digitos.endsWith("%d%d".formatted(dv1, dv2));
    }

    private static int calcularDigito(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += Character.getNumericValue(base.charAt(i)) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
