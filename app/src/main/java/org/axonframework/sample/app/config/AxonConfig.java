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

import org.axonframework.commandhandling.AnnotationCommandHandlerAdapter;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.common.jpa.ContainerManagedEntityManagerProvider;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventhandling.SimpleEventHandlerInvoker;
import org.axonframework.eventhandling.SubscribingEventProcessor;
import org.axonframework.eventsourcing.AggregateSnapshotter;
import org.axonframework.eventsourcing.EventCountSnapshotterTrigger;
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;
import org.axonframework.sample.app.command.Contact;
import org.axonframework.sample.app.command.ContactCommandHandler;
import org.axonframework.sample.app.command.ContactNameRepository;
import org.axonframework.sample.app.command.JpaContactNameRepository;
import org.axonframework.sample.app.query.AddressTableUpdater;
import org.axonframework.sample.app.query.ContactRepository;
import org.axonframework.spring.eventsourcing.SpringAggregateSnapshotterFactoryBean;
import org.axonframework.spring.eventsourcing.SpringPrototypeAggregateFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@Configuration
@Import({DatabaseConfig.class, DataInitConfig.class})
public class AxonConfig {

    @Autowired
    private ContactRepository queryContactRepository;

    @Autowired
    private TransactionManager transactionManager;

    @Autowired
    private AddressTableUpdater addressTableUpdater;

    @Autowired
    private AggregateSnapshotter aggregateSnapshotter;

    @Bean
    public CommandBus commandBus() throws Exception {
        SimpleCommandBus commandBus = new SimpleCommandBus();
        commandBus.setTransactionManager(transactionManager);

        return commandBus;
    }

    @Bean
    public EntityManagerProvider entityManagerProvider() {
        return new ContainerManagedEntityManagerProvider();
    }

    @Bean
    public EventStorageEngine eventStorageEngine() {
        return new JpaEventStorageEngine(entityManagerProvider(), transactionManager);
    }

    @Bean
    public EmbeddedEventStore eventStore() {
        return new EmbeddedEventStore(eventStorageEngine());
    }

    @Bean
    public ContactCommandHandler contactCommandHandler() {
        ContactCommandHandler commandHandler = new ContactCommandHandler();
        commandHandler.setRepository(contactRepository());
        commandHandler.setContactRepository(queryContactRepository);
        commandHandler.setContactNameRepository(contactNameRepository());

        return commandHandler;
    }

    @Bean
    public ContactNameRepository contactNameRepository() {
        return new JpaContactNameRepository();
    }

    @Bean
    public AnnotationCommandHandlerAdapter annotationCommandHandlerAdapter() throws Exception {
        AnnotationCommandHandlerAdapter annotationCommandHandlerAdapter =
                new AnnotationCommandHandlerAdapter(contactCommandHandler());

        annotationCommandHandlerAdapter.subscribe(commandBus());

        return annotationCommandHandlerAdapter;
    }

    @Bean
    @Scope("prototype")
    public Contact contact() {
        return new Contact();
    }

    @Bean
    public SpringAggregateSnapshotterFactoryBean springAggregateSnapshotterFactoryBean() {
        return new SpringAggregateSnapshotterFactoryBean();
    }

    @Bean
    public SpringPrototypeAggregateFactory<Contact> springPrototypeAggregateFactory() {
        SpringPrototypeAggregateFactory<Contact> aggregateFactory = new SpringPrototypeAggregateFactory<>();
        aggregateFactory.setPrototypeBeanName("contact");

        return aggregateFactory;
    }

    @Bean
    public EventSourcingRepository<Contact> contactRepository() {
        EventSourcingRepository<Contact> repository = new EventSourcingRepository<>(Contact.class, eventStore());

        EventCountSnapshotterTrigger snapshotterTrigger = new EventCountSnapshotterTrigger();
        snapshotterTrigger.setTrigger(5);
        snapshotterTrigger.setSnapshotter(aggregateSnapshotter);
        repository.setSnapshotterTrigger(snapshotterTrigger);

        return repository;
    }

    @Bean
    public SubscribingEventProcessor eventProcessor() {
        SubscribingEventProcessor eventProcessor = new SubscribingEventProcessor("eventProcessor",
                                                                                 new SimpleEventHandlerInvoker(
                                                                                         addressTableUpdater),
                                                                                 eventStore());
        eventProcessor.start();
        return eventProcessor;
    }
}