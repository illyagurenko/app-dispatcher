package ru.itone.illya4gurenko.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.itone.illya4gurenko.entity.AppDispatcherPermission
import java.util.UUID

@Repository
interface PermissionRepository : JpaRepository<AppDispatcherPermission, UUID> {
    fun findByLogin(login: String): AppDispatcherPermission?
}