package be.reactiveprogramming.examples.reactivereservations.entity;

public class BannedCreditcard {

  private final String ccNumber;

  public BannedCreditcard(String ccNumber) {
    this.ccNumber = ccNumber;
  }

  public String getCcNumber() {
    return ccNumber;
  }
}
