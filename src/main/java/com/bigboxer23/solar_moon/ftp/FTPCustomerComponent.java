package com.bigboxer23.solar_moon.ftp;

import com.bigboxer23.solar_moon.customer.CustomerComponent;
import com.bigboxer23.solar_moon.data.Customer;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** */
@Component
public class FTPCustomerComponent extends CustomerComponent {
	public Map<String, String> getCustomerAccessKeyMap() {
		return getTable().scan().items().stream()
				.collect(Collectors.toMap(Customer::getCustomerId, Customer::getAccessKey));
	}
}
