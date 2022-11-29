package com.example.transactionsmultiplesdbs;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {
  @Autowired
  @Qualifier("transactionManagerApp")
  private PlatformTransactionManager transactionManager;

  @Bean
  public Job job(Step step, JobRepository jobRepository) {
    return new JobBuilder("job", jobRepository)
        .start(step)
        .incrementer(new RunIdIncrementer())
        .build();
  }

  @Bean
  public Step step(ItemReader<Pessoa> reader, ItemWriter<Pessoa> writer, JobRepository jobRepository) {
    return new StepBuilder("step", jobRepository)
        .<Pessoa, Pessoa>chunk(200, transactionManager)
        .reader(reader)
        .writer(writer)
        .build();
  }

  @Bean
  public ItemReader<Pessoa> reader() {
    return new FlatFileItemReaderBuilder<Pessoa>()
        .name("reader")
        .resource(new FileSystemResource("files/pessoas.csv"))
        .comments("--")
        .delimited()
        .names("nome", "email", "dataNascimento", "idade", "id")
        .targetType(Pessoa.class)
        .build();
  }

  @Bean
  public ItemWriter<Pessoa> writer(@Qualifier("appDS") DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<Pessoa>()
        .dataSource(dataSource)
        .sql(
            "INSERT INTO pessoa (id, nome, email, data_nascimento, idade) VALUES (:id, :nome, :email, :dataNascimento, :idade)")
        .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
        .build();
  }
}
