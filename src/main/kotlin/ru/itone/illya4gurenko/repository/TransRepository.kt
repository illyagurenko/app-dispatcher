package ru.itone.illya4gurenko.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.itone.illya4gurenko.entity.AppDispatcherTrans
import java.util.UUID

@Repository
interface TransRepository : JpaRepository<AppDispatcherTrans, UUID> {

}