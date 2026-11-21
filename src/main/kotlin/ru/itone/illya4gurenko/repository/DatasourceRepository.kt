package ru.itone.illya4gurenko.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.itone.illya4gurenko.entity.AppDispatcherDatasource
import java.util.UUID

@Repository
interface DatasourceRepository : JpaRepository<AppDispatcherDatasource, UUID> {
    fun findAllByAccountInAndStatus(accounts: Collection<String>, status: String): List<AppDispatcherDatasource>
}