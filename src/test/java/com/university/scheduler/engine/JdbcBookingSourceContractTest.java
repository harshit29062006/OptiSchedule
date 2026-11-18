package com.university.scheduler.engine;

import com.university.scheduler.dao.JdbcTestDatabase;

class JdbcBookingSourceContractTest extends BookingSourceContractTest {
    @Override protected BookingSource createSource(){
        JdbcTestDatabase db=new JdbcTestDatabase();db.seedBooking();return new JdbcBookingSource(db);
    }
}
