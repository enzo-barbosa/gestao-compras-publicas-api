# language: pt
Funcionalidade: Geração mensal de empenhos com rateio do contrato
  Como gestor de compras públicas
  Quero empenhar mensalmente o valor rateado dos contratos vigentes
  Para que o compromisso financeiro respeite, mês a mês, o saldo da dotação orçamentária e o saldo restante do contrato
    E para que a dotação só seja reservada na competência do desembolso (corrente ou imediatamente anterior), nunca à frente

  Contexto: Contrato ativo com dotação limitada
    Dado um contrato "014/2026" no valor total de R$ 60.000,00 com duração de 6 meses
      E que o contrato está VIGENTE de 01/01/2026 a 30/06/2026
      E uma dotação "3.3.90.30" com saldo atual de R$ 25.000,00
      E que o valor mensal do contrato é R$ 10.000,00
      E que estamos na competência corrente 03/2026 (02/2026 pendente; 01/2026 já vencida)
      E que a competência 02/2026 já está empenhada

  Cenário: Gerar empenho mensal com sucesso
    Quando eu gero o empenho da competência 03/2026 para o contrato "014/2026"
    Então o empenho é criado com valor de R$ 10.000,00 e status EMPENHADO
      E o saldo atual da dotação passa a ser R$ 15.000,00
      E o saldo restante do contrato passa a ser R$ 50.000,00
      E é registrada uma movimentação de DÉBITO na dotação

  Cenário: Impedir dois empenhos na mesma competência
    Dado que já existe um empenho do contrato para a competência 03/2026
    Quando eu tento gerar outro empenho da competência 03/2026
    Então recebo erro 409 informando duplicidade de competência
      E nenhum débito é realizado na dotação

  Cenário: Impedir empenho em competência futura
    Quando eu gero o empenho da competência 04/2026
    Então recebo erro 409 informando que só é permitida a competência corrente (03/2026) ou, se pendente, a imediatamente anterior (02/2026)

  Cenário: Impedir empenho em competência já vencida (além da imediatamente anterior)
    Quando eu gero o empenho da competência 01/2026
    Então recebo erro 409 informando que só é permitida a competência corrente (03/2026) ou, se pendente, a imediatamente anterior (02/2026)

  Cenário: Impedir empenho fora da vigência do contrato
    Quando eu gero o empenho da competência 07/2026
    Então recebo erro 409 informando que a competência está fora da vigência (01/2026 a 06/2026)

  Cenário: Impedir empenho para contrato não vigente
    Dado que o contrato "014/2026" está RESCINDIDO
    Quando eu gero o empenho da competência 03/2026
    Então recebo erro 409 informando que o contrato não permite novos empenhos

  Cenário: Saldo insuficiente na dotação orçamentária
    Dado que a dotação "3.3.90.30" possui saldo atual de apenas R$ 5.000,00
    Quando eu gero o empenho da competência 03/2026
    Então recebo erro 400 informando saldo insuficiente na dotação
      E nenhuma movimentação é registrada
      E nenhum empenho é criado

  Cenário: Saldo restante insuficiente no contrato
    Dado que o contrato "014/2026" possui saldo restante de R$ 9.000,00
      E que a dotação "3.3.90.30" possui saldo atual suficiente de R$ 25.000,00
    Quando eu gero o empenho da competência 03/2026
    Então recebo erro 400 informando saldo restante insuficiente no contrato

  Cenário: Anulação de empenho estorna os saldos comprometidos
    Dado um empenho EMPENHADO de R$ 10.000,00 da competência 03/2026
      E que o saldo atual da dotação é R$ 15.000,00
      E que o saldo restante do contrato é R$ 50.000,00
    Quando eu anulo o empenho
    Então o empenho passa ao status ANULADO
      E o saldo atual da dotação volta a ser R$ 25.000,00
      E o saldo restante do contrato volta a ser R$ 60.000,00
      E é registrada uma movimentação de CRÉDITO na dotação

  Cenário: Impedir anulação de empenho liquidado ou pago
    Dado um empenho LIQUIDADO da competência 03/2026
    Quando eu tento anulá-lo
    Então recebo erro 409 informando que empenhos liquidados ou pagos não podem ser anulados

  Cenário: Anulação concorrente do mesmo empenho estorna os saldos exatamente uma vez
    Dado um empenho EMPENHADO de R$ 10.000,00 da competência 03/2026
      E que o saldo atual da dotação é R$ 15.000,00
      E que o saldo restante do contrato é R$ 50.000,00
    Quando duas anulações do mesmo empenho são disparadas simultaneamente
    Então apenas uma anulação é aceita e a outra recebe erro 409
      E o saldo atual da dotação é estornado uma única vez (volta a R$ 25.000,00)
      E o saldo restante do contrato é estornado uma única vez (volta a R$ 60.000,00)
