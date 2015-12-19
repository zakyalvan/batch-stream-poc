package com.jakartawebs.poc;

import java.util.Arrays;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.batch.integration.launch.JobLaunchingGateway;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.orm.JpaQueryProvider;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.dsl.jms.Jms;
import org.springframework.integration.dsl.support.Transformers;
import org.springframework.integration.mail.MailSendingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.mail.MailMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Configuration
@EnableBatchProcessing
@EnableIntegration
@IntegrationComponentScan
public class StreamingConfiguration {
	/**
	 * Launching batch job configuration.
	 * 
	 * @author zakyalvan
	 * @since 1.0
	 */
	@Configuration
	public static class JobLaunchFlowConfiguration {
		@Autowired
		private JobLauncher jobLauncher;
		
		@Bean
		public MessageSource<PushInfo> pushInfoSource() {
			return new MessageSource<PushInfo>() {
				private static final String COUNT_QUERY_STRING = "SELECT COUNT(pi) FROM PushInfo AS pi WHERE pi.lastExecution=:lastExecution";
				private static final String FETCH_QUERY_STRING = "FROM PushInfo AS pi WHERE pi.lastExecution=:lastExecution";
				
				@PersistenceContext
				private EntityManager entityManager;
				
				@Override
				@Transactional(propagation=Propagation.REQUIRED)
				public Message<PushInfo> receive() {
					Long pushInfoCount = entityManager.createQuery(COUNT_QUERY_STRING, Long.class)
							.setParameter("lastExecution", true)
							.getSingleResult();
					
					if(pushInfoCount == 0) {
						Date startTimestamp = new Date();
						startTimestamp.setTime(0);

						Date endTimestamp = new Date();
						PushInfo pushInfo = new PushInfo(startTimestamp, endTimestamp);
						pushInfo.setLastExecution(true);
						return MessageBuilder.withPayload(entityManager.merge(pushInfo)).build();
					}
					else {
						PushInfo pushInfo = entityManager.createQuery(FETCH_QUERY_STRING, PushInfo.class)
								.setParameter("lastExecution", true)
								.getSingleResult();
						
						pushInfo.setLastExecution(false);
						entityManager.merge(pushInfo);
						
						PushInfo lastPushInfo = new PushInfo(pushInfo.getEndTimestamp(), new Date());
						lastPushInfo.setLastExecution(true);
						return MessageBuilder.withPayload(entityManager.merge(lastPushInfo)).build();
					}
				}
			};
		}
		
		@Bean
		public GenericTransformer<PushInfo, JobLaunchRequest> pushInfoToJobLaunchRequest() {
			return new GenericTransformer<PushInfo, JobLaunchRequest>() {
				@Autowired
				@Qualifier("pushSensorReadingJob")
				private Job pushSensorReadingJob;
				
				@Override
				public JobLaunchRequest transform(PushInfo source) {
					JobParametersBuilder parametersBuilder = new JobParametersBuilder();
					parametersBuilder.addDate("start.timestamp", source.getStartTimestamp());
					parametersBuilder.addDate("end.timestamp", source.getEndTimestamp());
					return new JobLaunchRequest(pushSensorReadingJob, parametersBuilder.toJobParameters());
				}
			};
		}
		
		@Bean
		public JobLaunchingGateway jobLaunchingGateway() {
			return new JobLaunchingGateway(jobLauncher);
		}
		
		@Bean
		public GenericTransformer<JobExecution, MailMessage> jobExecutionToMailMessage() {
			return new GenericTransformer<JobExecution, MailMessage>() {
				@Override
				public MailMessage transform(JobExecution source) {
					SimpleMailMessage mailMessage = new SimpleMailMessage();
					mailMessage.setFrom("lamonepoda@gmail.com");
					mailMessage.setTo("zakyalvan@gmail.com");
					
					mailMessage.setSubject("Push Job Execution Summary");
					
					StringBuilder textBuilder = new StringBuilder();
					textBuilder.append("Following are summary of push sensor reading batch job :");
					
					mailMessage.setText(textBuilder.toString());
					return mailMessage;
				}
				
			};
		}
		
		@Autowired(required=false)
		private JavaMailSender javaMailSender;
		
		@Bean
		@ConditionalOnBean(value=JavaMailSender.class)
		public MessageHandler mailSendingMessageHandler() {
			return new MailSendingMessageHandler(javaMailSender);
		}
		
		@Bean
		public IntegrationFlow launchJobFlow() {
			return IntegrationFlows.from(pushInfoSource(), sourceSpec -> sourceSpec.poller(Pollers.cron("0/30 * * * * ?")))
					.transform(pushInfoToJobLaunchRequest())
					.handle(jobLaunchingGateway())
					.transform(jobExecutionToMailMessage())
					//.handle(mailSendingMessageHandler())
					.get();
		}
	}
	
	
	
	/**
	 * Batch job configuration.
	 * 
	 * @author zakyalvan
	 * @since 1.0
	 */
	@Configuration
	public static class BatchJobConfiguration {
		@PersistenceContext
		private EntityManager entityManager;
		
		@Autowired
		private SensorReadingPusher dataPusher;
		
		@Bean @StepScope
		public JpaQueryProvider sensorReadingQueryProvider() {
			JpaQueryProvider sensorReadingQueryProvider = new JpaQueryProvider() {
				private EntityManager entityManager;
				
				@Value("#{jobParameters['start.timestamp']}")
				private Date startTimestamp;
				
				@Value("#{jobParameters['end.timestamp']}")
				private Date endTimestamp;
				
				@Override
				public Query createQuery() {
					return entityManager.createQuery("FROM SensorReading AS sensor WHERE sensor.createdDate BETWEEN :startTimestamp AND :endTimestamp", SensorReading.class)
							.setParameter("startTimestamp", startTimestamp)
							.setParameter("endTimestamp", endTimestamp);
				}

				@Override
				public void setEntityManager(EntityManager entityManager) {
					this.entityManager = entityManager;
				}
				
				@PostConstruct
				public void check() {
					Assert.notNull(entityManager);
				}
			};
			sensorReadingQueryProvider.setEntityManager(entityManager);
			return sensorReadingQueryProvider;
		}
		
		@PersistenceUnit
		private EntityManagerFactory entityManagerFactory;
		
		@Bean(name="sensorsItemReader")
		public ItemReader<SensorReading> itemReader() {
			JpaPagingItemReader<SensorReading> itemreader = new JpaPagingItemReader<SensorReading>();
			itemreader.setQueryProvider(sensorReadingQueryProvider());
			itemreader.setEntityManagerFactory(entityManagerFactory);
			return itemreader;
		}

		@Bean(name="noopItemProcessor")
		public ItemProcessor<SensorReading, SensorReading> noopItemProcessor() {
			return new ItemProcessor<SensorReading, SensorReading>() {
				@Override
				public SensorReading process(SensorReading item) throws Exception {
					return item;
				}
			};
		}
		
		@Bean(name="sensorsItemProcessor")
		public ItemProcessor<SensorReading, SensorReading> itemProcessor() {
			CompositeItemProcessor<SensorReading, SensorReading> itemProcessor = new CompositeItemProcessor<>();
			itemProcessor.setDelegates(Arrays.asList(noopItemProcessor()));
			return itemProcessor;
		}

		@Bean(name="sensorsItemWriter")
		public ItemWriter<SensorReading> itemWriter() {
			return dataPusher;
		}
		
		@Bean(name="pushSensorReadingStep")
		public Step pushItemStep(StepBuilderFactory builderFactory,
				@Qualifier("sensorsItemReader") ItemReader<SensorReading> itemReader,
				@Qualifier("sensorsItemProcessor") ItemProcessor<SensorReading, SensorReading> itemProcessor,
				@Qualifier("sensorsItemWriter") ItemWriter<SensorReading> itemWriter) {
			return builderFactory.get("pushItemStep").<SensorReading, SensorReading>chunk(100).reader(itemReader).processor(itemProcessor).writer(itemWriter)
					.build();		
		}
		
		@Bean(name="pushSensorReadingJob")
		public Job pushItemJob(JobBuilderFactory builderFactory, Step pushItemStep) {
			return builderFactory.get("pushItemJob")
					.flow(pushItemStep).end()
					.build();
		}
	}
	
	/**
	 * Push data to remote system.
	 * 
	 * @author zakyalvan
	 * @since 1.0
	 */
	@Configuration
	public static class PushDataFlowConfiguration {
		@Autowired
		private ConnectionFactory connectionFactory;
		
		@Bean
		public MessageChannel startChannel() {
			return MessageChannels.direct().get();
		}
		
		@Bean
		public SensorReadingPusher sensorReadingPusher() {
			return new SensorReadingPusher(startChannel());
		}
		
		@Bean
		public IntegrationFlow pushDataFlow() {
			return IntegrationFlows.from(startChannel())
					.split()
					.transform(Transformers.toJson(JsonObjectMapperFactory.createObjectMapper()))
					.aggregate()
					.handle(Jms.outboundAdapter(connectionFactory).destination("aaa.sensor.reading.push.queue"))
					.get();
		}
	}
}
