package com.bigboxer23.solar_moon.ftp;

import com.bigboxer23.solar_moon.customer.DynamoDbCustomerRepository;
import com.bigboxer23.solar_moon.data.Customer;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

/** */
@Repository
public class FTPCustomerRepository extends DynamoDbCustomerRepository {
	@Override
	public DynamoDbTable<Customer> getTable() {
		return super.getTable();
	}
}
