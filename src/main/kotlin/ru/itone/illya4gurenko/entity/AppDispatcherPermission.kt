package ru.itone.illya4gurenko.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.ZonedDateTime
import java.util.UUID

/**
 * сущность учетных записей и прав доступа систем-клиентов
 */
@Entity
@Table(name = "APP_DISPATCHER_PERMISSION", schema = "ACC_APP_DISPATCHER")
class AppDispatcherPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", nullable = false, updatable = false)
    var id: UUID? = null

    @Column(name = "LOGIN", length = 256)
    var login: String? = null

    @Column(name = "PASS_BCRYPT", length = 256)
    var passBcrypt: String? = null

    @Column(name = "PERMISSION")
    var permission: Short? = null

    @CreationTimestamp
    @Column(name = "INS_TS", updatable = false)
    var insTs: ZonedDateTime? = null

    @UpdateTimestamp
    @Column(name = "UPD_TS")
    var updTs: ZonedDateTime? = null
}
