package com.vlife.cv

import org.apache.ibatis.session.SqlSessionFactory
import org.mybatis.spring.SqlSessionFactoryBean
import org.apache.ibatis.annotations.Mapper
import org.mybatis.spring.annotation.MapperScan
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

/**
 * 整合測試專用配置
 *
 * 提供測試所需的最小 Bean 配置
 */
@TestConfiguration
@MapperScan("com.vlife.cv", annotationClass = Mapper::class)
class TestConfiguration {

    /**
     * 提供 JdbcTemplate bean
     */
    @Bean
    fun jdbcTemplate(dataSource: DataSource): JdbcTemplate {
        return JdbcTemplate(dataSource)
    }

    /**
     * 配置 MyBatis SqlSessionFactory
     */
    @Bean
    fun sqlSessionFactory(dataSource: DataSource): SqlSessionFactory {
        val factory = SqlSessionFactoryBean()
        factory.setDataSource(dataSource)

        // 載入 Mapper XML
        val resolver = PathMatchingResourcePatternResolver()
        val mapperLocations = resolver.getResources("classpath:mapper/**/*.xml")
        factory.setMapperLocations(*mapperLocations)

        // 配置設定
        factory.setConfiguration(org.apache.ibatis.session.Configuration().apply {
            isMapUnderscoreToCamelCase = true
            defaultFetchSize = 100
            defaultStatementTimeout = 30
        })

        return factory.getObject()!!
    }
}
