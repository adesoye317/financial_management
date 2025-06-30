package com.financal.mgt.Financal.Management.repository;

import com.financal.mgt.Financal.Management.model.OtpRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends MongoRepository<OtpRequest, String> {
}

