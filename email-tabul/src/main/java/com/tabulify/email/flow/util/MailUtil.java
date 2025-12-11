package com.tabulify.email.flow.util;


import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MailUtil {

  /**
   * @param addressesList - a list of addresses separated by a comma
   * @return a list of internet address object
   * @throws AddressException - if the address is bad
   */
  public static List<InternetAddress> stringToListInternetAddress(String addressesList) throws AddressException {
    return Arrays.asList(InternetAddress.parse(addressesList));
  }

  /**
   * @param addressAndNames - a map of addresses and corresponding name
   * @return a list of internet address object
   * @throws AddressException             - if the address is bad
   * @throws UnsupportedEncodingException - if the name encoding can not be performed
   */
  public static List<InternetAddress> mapToListInternetAddress(Map<String, String> addressAndNames) throws AddressException, UnsupportedEncodingException {
    List<InternetAddress> internetAddresses = new ArrayList<>();
    for (Map.Entry<String, String> addressAndName : addressAndNames.entrySet()) {
      InternetAddress address = new InternetAddress(addressAndName.getKey());
      address.setPersonal(addressAndName.getValue(), String.valueOf(StandardCharsets.UTF_8));
      internetAddresses.add(address);
    }
    return internetAddresses;
  }

  /**
   * @param addresses    - a list of addresses
   * @param addressNames - a list of names
   * @return a list of internet address object
   * @throws AddressException             - if the address is bad
   * @throws UnsupportedEncodingException - if the name encoding can not be performed
   */
  public static List<InternetAddress> addressAndNamesToListInternetAddress(String addresses, String addressNames) throws AddressException, UnsupportedEncodingException {
    if (addresses == null) {
      return new ArrayList<>();
    }
    List<InternetAddress> internetAddresses = Arrays.asList(InternetAddress.parse(addresses));
    if (addressNames != null) {
      List<String> internetAddressesNames = Arrays.asList(addressNames.split(";"));
      for (int i = 0; i < internetAddresses.size(); i++) {
        String name = null;
        try {
          name = internetAddressesNames.get(i);
        } catch (IndexOutOfBoundsException e) {
          // no name
        }
        InternetAddress internetAddress = internetAddresses.get(i);
        internetAddress.setPersonal(name, String.valueOf(StandardCharsets.UTF_8));
      }
    }
    return internetAddresses;
  }

  public static InternetAddress addressAndNameToInternetAddress(String address, String name) throws AddressException, UnsupportedEncodingException {
    InternetAddress internetAddress = new InternetAddress(address);
    if (name != null) {
      internetAddress.setPersonal(name, String.valueOf(StandardCharsets.UTF_8));
    }
    return internetAddress;
  }
}
