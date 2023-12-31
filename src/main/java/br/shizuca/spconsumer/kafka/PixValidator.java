package br.shizuca.spconsumer.kafka;


import br.shizuca.spconsumer.dto.PixDTO;
import br.shizuca.spconsumer.dto.PixStatus;
import br.shizuca.spconsumer.exception.KeyNotFoundException;
import br.shizuca.spconsumer.model.KeyPix;
import br.shizuca.spconsumer.model.Pix;
import br.shizuca.spconsumer.repository.KeyRepository;
import br.shizuca.spconsumer.repository.PixRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;


@Service
public class PixValidator {

    @Autowired
    private KeyRepository keyRepository;

    @Autowired
    private PixRepository pixRepository;

    @KafkaListener(topics = "pix-topic", groupId = "grupo")
    @RetryableTopic(
            backoff = @Backoff(value = 3000L),
            attempts = "5",
            autoCreateTopics = "true",
            include = KeyNotFoundException.class
    )
    public void processaPix(PixDTO pixDTO, Acknowledgment acknowledgment) {
        System.out.println("Pix  recebido: " + pixDTO.getIdentifier());

        Pix pix = pixRepository.findByIdentifier(pixDTO.getIdentifier());

        KeyPix origem = keyRepository.findByChave(pixDTO.getChaveOrigem());
        KeyPix destino = keyRepository.findByChave(pixDTO.getChaveDestino());

        if (origem == null || destino == null) {
            pix.setStatus(PixStatus.ERRO);
            throw new KeyNotFoundException();
        } else {
            pix.setStatus(PixStatus.PROCESSADO);
        }
        pixRepository.save(pix);

        acknowledgment.acknowledge();
    }

}
