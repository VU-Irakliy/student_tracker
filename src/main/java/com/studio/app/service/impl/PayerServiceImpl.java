package com.studio.app.service.impl;

import com.studio.app.dto.request.PayerRequest;
import com.studio.app.dto.response.PayerResponse;
import com.studio.app.entity.Payer;
import com.studio.app.exception.ResourceNotFoundException;
import com.studio.app.mapper.PayerMapper;
import com.studio.app.repository.PayerRepository;
import com.studio.app.repository.StudentRepository;
import com.studio.app.service.PayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link PayerService}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PayerServiceImpl implements PayerService {

    private final PayerRepository payerRepository;
    private final StudentRepository studentRepository;
    private final PayerMapper payerMapper;

    /** {@inheritDoc} */
    @Override
    public PayerResponse addPayer(Long studentId, PayerRequest request) {
        var student = studentRepository.findByIdAndDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        var payer = Payer.builder()
                .student(student)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .note(request.getNote())
                .build();

        return payerMapper.toResponse(payerRepository.save(payer));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<PayerResponse> getPayersForStudent(Long studentId) {
        return payerMapper.toResponseList(
                payerRepository.findByStudentIdAndDeletedFalse(studentId));
    }

    /** {@inheritDoc} */
    @Override
    public PayerResponse updatePayer(Long studentId, Long payerId, PayerRequest request) {
        var payer = payerRepository.findByIdAndStudentIdAndDeletedFalse(payerId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payer", payerId));

        Optional.ofNullable(request.getFullName()).ifPresent(payer::setFullName);
        Optional.ofNullable(request.getPhoneNumber()).ifPresent(payer::setPhoneNumber);
        Optional.ofNullable(request.getNote()).ifPresent(payer::setNote);

        return payerMapper.toResponse(payerRepository.save(payer));
    }

    /** {@inheritDoc} */
    @Override
    public void removePayer(Long studentId, Long payerId) {
        var payer = payerRepository.findByIdAndStudentIdAndDeletedFalse(payerId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payer", payerId));

        payer.setDeleted(true);
        payerRepository.save(payer);
    }
}
