/*
 * Copyright (c) 2010-2016. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.sample.app.config;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.axonframework.sample.app.query.AddressTableUpdater;
import org.axonframework.sample.app.query.ContactRepository;
import org.axonframework.sample.app.query.ContactRepositoryImpl;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.beans.PropertyVetoException;

@Configuration
@EnableTransactionManagement
@PropertySource("classpath:hsqldb.database.properties")
public class DatabaseConfig implements EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    @Bean
    public AddressTableUpdater addressTableUpdater() {
        return new AddressTableUpdater();
    }

    @Bean
    public ContactRepository queryContactRepository() {
        return new ContactRepositoryImpl();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() throws PropertyVetoException {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setPersistenceUnitName("addresses");

        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setDatabasePlatform(environment.getProperty("hibernate.sql.dialect"));
        jpaVendorAdapter.setGenerateDdl(environment.getProperty("hibernate.sql.generateddl", Boolean.class));
        jpaVendorAdapter.setShowSql(environment.getProperty("hibernate.sql.show", Boolean.class));

        entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);

        entityManagerFactoryBean.setDataSource(dataSource());

        return entityManagerFactoryBean;
    }

    @Bean
    public JpaTransactionManager transactionManager() throws PropertyVetoException {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());

        return transactionManager;
    }

    @Bean
    public ComboPooledDataSource dataSource() throws PropertyVetoException {
        ComboPooledDataSource comboPooledDataSource = new ComboPooledDataSource();
        comboPooledDataSource.setDriverClass(environment.getProperty("jdbc.driverclass"));
        comboPooledDataSource.setJdbcUrl(environment.getProperty("jdbc.url"));
        comboPooledDataSource.setUser(environment.getProperty("jdbc.username"));
        comboPooledDataSource.setPassword(environment.getProperty("jdbc.password"));
        comboPooledDataSource.setMaxPoolSize(15);
        comboPooledDataSource.setMinPoolSize(5);
        return comboPooledDataSource;
    }

    @Bean
    public PersistenceAnnotationBeanPostProcessor persistenceAnnotationBeanPostProcessor() {
        return new PersistenceAnnotationBeanPostProcessor();
    }
}
