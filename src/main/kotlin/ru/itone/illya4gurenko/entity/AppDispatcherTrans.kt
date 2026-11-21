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
@Table(name = "APP_DISPATCHER_TRANS", schema = "ACC_APP_DISPATCHER")
class AppDispatcherTrans {
    @Id
    @Column(name = "ID", length = 256, nullable = false)
    var id: UUID? = null

    @Column(name = "PROTOCOL_TYPE", length = 256)
    var protocolType: String? = null

    @Column(name = "SYSTEM_ID", length = 256)
    var systemId: String? = null

    @Column(name = "RESP_DATA", columnDefinition = "TEXT")
    var respData: String? = null

    @Column(name = "REQ_DATA", columnDefinition = "TEXT")
    var reqData: String? = null

    @Column(name = "STATUS", length = 256)
    var status: String? = null

    @Column(name = "RESP_CODE", length = 16)
    var respCode: String? = null

    @Column(name = "RESP_DESC", length = 256)
    var respDesc: String? = null

    @CreationTimestamp
    @Column(name = "INS_TS", updatable = false)
    var insTs: ZonedDateTime? = null

    @UpdateTimestamp
    @Column(name = "UPD_TS")
    var updTs: ZonedDateTime? = null
}