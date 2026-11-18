package com.university.scheduler.engine;

/**
 * The CSV source is the reference implementation of the contract.
 * Member A's JdbcBookingSource should get an equivalent class that extends
 * BookingSourceContractTest and returns a JdbcBookingSource from createSource().
 */
class CsvBookingSourceContractTest extends BookingSourceContractTest {

    @Override
    protected BookingSource createSource() {
        return new CsvBookingSource(OptimizationRunDriver.DEFAULT_DATA);
    }
}
