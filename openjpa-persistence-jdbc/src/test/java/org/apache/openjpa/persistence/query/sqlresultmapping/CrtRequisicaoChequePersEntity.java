/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.persistence.query.sqlresultmapping;

import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.ManyToOne;
import javax.persistence.SqlResultSetMapping;

@javax.persistence.Table(name = "CRT_REQUISICAO_CHEQUE_PERS")
@Entity
@SqlResultSetMapping(name = "MyResultMapping", entities = {
    @EntityResult(entityClass = CrtRequisicaoChequePersEntity.class, fields = {
        @FieldResult(name = "crtOperacaoByOperacaoRecepcaoServCent.id",
                column = "opRecepcaoServCentraisId"),
        @FieldResult(name = "crtOperacaoByOperacaoRecepcaoServCent.dataHora",
                column = "opRecepcaoServCentraisDataHora") }) })
public class CrtRequisicaoChequePersEntity extends CrtRequisicaoEntity {

    @ManyToOne
    @javax.persistence.JoinColumn(name = "OPERACAO_RECEPCAO_SERV_CENT", referencedColumnName = "ID")
    private CrtOperacaoEntity crtOperacaoByOperacaoRecepcaoServCent;

    public CrtOperacaoEntity getCrtOperacaoByOperacaoRecepcaoServCent() {
        return this.crtOperacaoByOperacaoRecepcaoServCent;
    }

    public void setCrtOperacaoByOperacaoRecepcaoServCent(
        final CrtOperacaoEntity crtOperacaoByOperacaoRecepcaoServCent) {
        this.crtOperacaoByOperacaoRecepcaoServCent =
            crtOperacaoByOperacaoRecepcaoServCent;
    }

    @ManyToOne
    @javax.persistence.JoinColumn(name = "OPERACAO_REQUISICAO", referencedColumnName = "ID", nullable = false)
    private CrtOperacaoEntity crtOperacaoByOperacaoRequisicao;

    public CrtOperacaoEntity getCrtOperacaoByOperacaoRequisicao() {
        return this.crtOperacaoByOperacaoRequisicao;
    }

    public void setCrtOperacaoByOperacaoRequisicao(
        final CrtOperacaoEntity crtOperacaoByOperacaoRequisicao) {
        this.crtOperacaoByOperacaoRequisicao = crtOperacaoByOperacaoRequisicao;
    }
}
