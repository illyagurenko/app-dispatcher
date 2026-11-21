package ru.itone.illya4gurenko.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "APP_DISPATCHER_DATASOURCE", schema = "ACC_APP_DISPATCHER")
class AppDispatcherDatasource {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", nullable = false, updatable = false)
    var id: UUID? = null

    @Column(name = "ACCOUNT", length = 255)
    var account: String? = null

    @Column(name = "JDBC", length = 255)
    var jdbc: String? = null

    @Column(name = "JDBC_TYPE", length = 255)
    var jdbcType: String? = null

    @Column(name = "STATUS", length = 255)
    var status: String? = null

    @CreationTimestamp
    @Column(name = "INS_TS", updatable = false)
    var insTs: ZonedDateTime? = null

    @UpdateTimestamp
    @Column(name = "UPD_TS")
    var updTs: ZonedDateTime? = null
}