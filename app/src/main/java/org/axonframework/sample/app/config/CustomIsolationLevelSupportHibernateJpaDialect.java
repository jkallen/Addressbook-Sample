/*
 * Copyright (c) 2016. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.sample.app.config;

import org.hibernate.Session;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

class CustomIsolationLevelSupportHibernateJpaDialect extends HibernateJpaDialect {

    @Override
    public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
            throws PersistenceException, SQLException, TransactionException {

        Session session = (Session) entityManager.getDelegate();

        final SessionTransactionData sessionTransactionData = new SessionTransactionData();

        session.doWork(connection -> {
            Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(connection, definition);
            sessionTransactionData.setConnection(connection);
            sessionTransactionData.setPreviousIsolationLevel(previousIsolationLevel);
        });

        entityManager.getTransaction()
                     .begin();

        Object springSessionTransactionData = super.prepareTransaction(entityManager,
                                                                       definition.isReadOnly(),
                                                                       definition.getName());

        sessionTransactionData.setSpringSessionTransactionData(springSessionTransactionData);

        return sessionTransactionData;
    }

    @Override
    public Object prepareTransaction(EntityManager entityManager, boolean readOnly, String name)
            throws PersistenceException {
        return super.prepareTransaction(entityManager, readOnly, name);
    }

    @Override
    public void cleanupTransaction(Object transactionData) {
        SessionTransactionData sessionTransactionData = (SessionTransactionData) transactionData;
        DataSourceUtils.resetConnectionAfterTransaction(sessionTransactionData.getConnection(), sessionTransactionData.getPreviousIsolationLevel());

        super.cleanupTransaction(sessionTransactionData.getSpringSessionTransactionData());
    }

    private class SessionTransactionData {

        private Connection connection;
        private Integer previousIsolationLevel;
        private Object springSessionTransactionData;

        public void setSpringSessionTransactionData(Object springSessionTransactionData) {
            this.springSessionTransactionData = springSessionTransactionData;
        }

        public void setConnection(Connection connection) {
            this.connection = connection;
        }

        public void setPreviousIsolationLevel(Integer previousIsolationLevel) {
            this.previousIsolationLevel = previousIsolationLevel;
        }

        public Connection getConnection() {
            return connection;
        }

        public Integer getPreviousIsolationLevel() {
            return previousIsolationLevel;
        }

        public Object getSpringSessionTransactionData() {
            return springSessionTransactionData;
        }
    }
}