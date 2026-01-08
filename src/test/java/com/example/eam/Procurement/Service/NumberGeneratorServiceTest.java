package com.example.eam.Procurement.Service;

import com.example.eam.Procurement.Entity.ProcurementNumberSequence;
import com.example.eam.Procurement.Repository.ProcurementNumberSequenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NumberGeneratorServiceTest {

    @Mock
    private ProcurementNumberSequenceRepository repository;

    @InjectMocks
    private NumberGeneratorService service;

    @Test
    void generateMrNumber_incrementsExistingSequence() {
        int year = Year.now().getValue();
        ProcurementNumberSequence seq = ProcurementNumberSequence.builder()
                .sequenceKey("MR")
                .year(year)
                .lastValue(5L)
                .build();

        when(repository.findBySequenceKeyAndYear("MR", year)).thenReturn(Optional.of(seq));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String result = service.generateMrNumber();

        assertEquals("MR-" + year + "-0006", result);
    }

    @Test
    void generatePoNumber_throwsWhenSequenceExhausted() {
        int year = Year.now().getValue();
        ProcurementNumberSequence seq = ProcurementNumberSequence.builder()
                .sequenceKey("PO")
                .year(year)
                .lastValue(10000L)
                .build();

        when(repository.findBySequenceKeyAndYear("PO", year)).thenReturn(Optional.of(seq));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, service::generatePoNumber);
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
