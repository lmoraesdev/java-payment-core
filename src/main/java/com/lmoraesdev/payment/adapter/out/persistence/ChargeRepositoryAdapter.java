package com.lmoraesdev.payment.adapter.out.persistence;

import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.domain.model.Charge;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ChargeRepositoryAdapter implements ChargeRepository{
    private final SpringDataChargeRepository repository;

    public ChargeRepositoryAdapter(SpringDataChargeRepository repository){
        this.repository = repository;
    }

    @Override
    public Charge save(Charge charge){
        ChargeJpaEntity entity = ChargeMapper.toEntity(charge);

        ChargeJpaEntity saved = repository.save(entity);

        return ChargeMapper.toDomain(saved);
    }

    @Override
    public Optional<Charge> findById(UUID id){
        return repository.findById(id)
                .map(ChargeMapper::toDomain);
    }

}
