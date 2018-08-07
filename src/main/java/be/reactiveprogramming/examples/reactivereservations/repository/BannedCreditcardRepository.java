package be.reactiveprogramming.examples.reactivereservations.repository;

import be.reactiveprogramming.examples.reactivereservations.entity.BannedCreditcard;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface BannedCreditcardRepository extends ReactiveMongoRepository<BannedCreditcard, String> {

}
