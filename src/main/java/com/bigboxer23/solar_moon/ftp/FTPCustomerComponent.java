package com.bigboxer23.solar_moon.ftp;

import com.bigboxer23.solar_moon.customer.CustomerComponent;
import com.bigboxer23.solar_moon.data.Customer;
import java.util.List;
import org.springframework.stereotype.Component;

/** */
@Component
public class FTPCustomerComponent extends CustomerComponent {
	@Override
	public FTPCustomerRepository getRepository() {
		return (FTPCustomerRepository) super.getRepository();
	}

	public List<String> getAccessKeys() {
		return getRepository().getTable().scan().items().stream()
				.map(Customer::getAccessKey)
				.toList();
	}
}
