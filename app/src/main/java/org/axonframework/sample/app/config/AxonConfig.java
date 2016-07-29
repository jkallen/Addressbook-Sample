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

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.commandhandling.annotation.AnnotationCommandHandlerBeanPostProcessor;
import org.axonframework.contextsupport.spring.TransactionManagerFactoryBean;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.SimpleEventBus;
import org.axonframework.eventhandling.annotation.AnnotationEventListenerBeanPostProcessor;
import org.axonframework.eventsourcing.AggregateFactory;
import org.axonframework.eventsourcing.EventCountSnapshotterTrigger;
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.axonframework.eventsourcing.Snapshotter;
import org.axonframework.eventsourcing.SpringAggregateSnapshotter;
import org.axonframework.eventsourcing.SpringPrototypeAggregateFactory;
import org.axonframework.eventstore.fs.FileSystemEventStore;
import org.axonframework.eventstore.fs.SimpleEventFileResolver;
import org.axonframework.sample.app.command.Contact;
import org.axonframework.sample.app.command.ContactCommandHandler;
import org.axonframework.sample.app.command.ContactNameRepository;
import org.axonframework.sample.app.command.JpaContactNameRepository;
import org.axonframework.sample.app.query.ContactRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Import({DatabaseConfig.class, DataInitConfig.class})
public class AxonConfig {

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(2);
        threadPoolTaskExecutor.setMaxPoolSize(5);
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);

        return threadPoolTaskExecutor;
    }

    @Bean
    public EventBus eventBus() {
        return new SimpleEventBus();
    }

    @Bean
    public Snapshotter snapshotter() {
        SpringAggregateSnapshotter snapshotter = new SpringAggregateSnapshotter();
        snapshotter.setEventStore(eventStore());
        snapshotter.setExecutor(taskExecutor());

        AggregateFactory<Contact> contactAggregateFactory = springPrototypeAggregateFactory();
        List<AggregateFactory<?>> aggregateFactories = new ArrayList<AggregateFactory<?>>();
        aggregateFactories.add(contactAggregateFactory);
        snapshotter.setAggregateFactories(aggregateFactories);

        return snapshotter;
    }

    @Bean
    public FileSystemEventStore eventStore() {
        String property = System.getProperty("java.io.tmpdir");
        return new FileSystemEventStore(new SimpleEventFileResolver(new File(property)));
    }

    @Bean
    public ContactCommandHandler contactCommandHandler(EventSourcingRepository<Contact> repository,
                                                       ContactRepository queryContactRepository) {
        ContactCommandHandler commandHandler = new ContactCommandHandler();
        commandHandler.setRepository(repository);
        commandHandler.setContactRepository(queryContactRepository);
        commandHandler.setContactNameRepository(contactNameRepository());

        return commandHandler;
    }

    @Bean
    public ContactNameRepository contactNameRepository() {
        return new JpaContactNameRepository();
    }

    @Bean
    @Scope("prototype")
    public Contact contact() {
        return new Contact();
    }

    @Bean
    public AggregateFactory<Contact> springPrototypeAggregateFactory() {
        SpringPrototypeAggregateFactory<Contact> aggregateFactory = new SpringPrototypeAggregateFactory<Contact>();
        aggregateFactory.setPrototypeBeanName("contact");
        aggregateFactory.setTypeIdentifier("Contact");

        return aggregateFactory;
    }

    @Bean
    public EventSourcingRepository<Contact> contactRepository() {
        EventSourcingRepository<Contact> repository = new EventSourcingRepository<Contact>(Contact.class, eventStore());
        repository.setEventBus(eventBus());

        EventCountSnapshotterTrigger snapshotterTrigger = new EventCountSnapshotterTrigger();
        snapshotterTrigger.setTrigger(5);
        snapshotterTrigger.setSnapshotter(snapshotter());
        repository.setSnapshotterTrigger(snapshotterTrigger);

        return repository;
    }

    @Bean
    public CommandBus commandBus(JpaTransactionManager transactionManager) throws Exception {
        SimpleCommandBus simpleCommandBus = new SimpleCommandBus();

        TransactionManagerFactoryBean factoryBean = new TransactionManagerFactoryBean();
        factoryBean.setTransactionManager(transactionManager);
        simpleCommandBus.setTransactionManager(factoryBean.getObject());

        return simpleCommandBus;
    }

    @Bean
    public AnnotationCommandHandlerBeanPostProcessor annotationCommandHandlerBeanPostProcessor() {
        return new AnnotationCommandHandlerBeanPostProcessor();
    }

    @Bean
    public AnnotationEventListenerBeanPostProcessor annotationEventListenerBeanPostProcessor() {
        return new AnnotationEventListenerBeanPostProcessor();
    }
}
