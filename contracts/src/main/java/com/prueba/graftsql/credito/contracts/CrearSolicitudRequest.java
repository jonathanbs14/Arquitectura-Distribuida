package com.prueba.graftsql.credito.contracts;

import java.math.BigDecimal;

public record CrearSolicitudRequest(BigDecimal monto, int plazoMeses) {
}
