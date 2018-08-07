package be.reactiveprogramming.examples.reactivereservations;

import be.reactiveprogramming.examples.reactivereservations.entity.BannedCreditcard;
import be.reactiveprogramming.examples.reactivereservations.repository.BannedCreditcardRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.ReactorRabbitMq;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
@EnableMongoAuditing
@EnableReactiveMongoRepositories
public class App {

  private static final String queue = "bannedCreditCards";
  private static final Random r = new Random();

  public static void main(String[] args) throws Exception {
    final ConfigurableApplicationContext applicationContext = SpringApplication.run(App.class, args);

    final BannedCreditcardRepository bannedCreditCardRepository = applicationContext.getBean(BannedCreditcardRepository.class);

    Mono<Void> emptyBannedCreditCardsFlow = bannedCreditCardRepository.deleteAll();

    Flux<BannedCreditcard> fillWithCreditCardsFlow =
        Flux.range(1, 100000)
            .filter(i -> i % 2 == 0)
            .map(i -> new BannedCreditcard("" + i))
            .flatMap(bcc -> bannedCreditCardRepository.save(bcc));

    emptyBannedCreditCardsFlow
        .thenMany(fillWithCreditCardsFlow)
        .blockLast(); //Demo purposes

    Sender sender = ReactorRabbitMq.createSender();

    sender.send(Flux.range(1, 1000).map(i -> new OutboundMessage("bannedCreditCardsValidations", "routing.key", randomCreditCardNumbers())))
        .subscribe();

    Receiver receiver = ReactorRabbitMq.createReceiver();

    receiver.consumeManualAck(queue, new ConsumeOptions().qos(5))
        .flatMap(msg -> validateCreditCards(msg, bannedCreditCardRepository))
        .subscribe(msg -> msg.ack(false));

    Thread.sleep(1000000000);
  }

  private static Mono<AcknowledgableDelivery> validateCreditCards(AcknowledgableDelivery msg, BannedCreditcardRepository bannedCreditCardRepository) {
    System.out.println("-------Checking new card set--------");
    List<String> creditCardsToCheck = Arrays.asList(new String(msg.getBody()).split("\\s*,\\s*"));

    return bannedCreditCardRepository.findAll()
        .filter(cc -> creditCardsToCheck.contains(cc.getCcNumber()))
        .map(cc -> printCreditCardInformation(cc))
        .then(Mono.just(msg));
    }

  private static BannedCreditcard printCreditCardInformation(BannedCreditcard cc) {
    System.out.println("Banned card: " + cc.getCcNumber());
    return cc;
  }

  private static byte[] randomCreditCardNumbers() {
    String randomList = "" + r.nextInt(100000);

    for(int i = 0; i < 4; i++) {
      randomList += "," + r.nextInt(100000);
    }

    return randomList.getBytes();
  }
}