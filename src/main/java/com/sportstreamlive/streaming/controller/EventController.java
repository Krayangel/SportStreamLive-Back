package com.sportstreamlive.streaming.controller;

import com.sportstreamlive.streaming.model.Event;
import com.sportstreamlive.streaming.repository.EventRepository;
import com.sportstreamlive.streaming.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * E3 - Eventos: Crear, inscribirse y listar eventos deportivos.
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventRepository eventRepository;
    private final BadgeService badgeService;

    /** GET /api/events */
    @GetMapping
    public List<Event> listAll() {
        return eventRepository.findAll();
    }

    /** GET /api/events/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Event> getById(@PathVariable String id) {
        return eventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/events — Crear evento (requiere autenticacion) */
    @PostMapping
    public ResponseEntity<Event> create(@RequestBody Event event) {
        Event saved = eventRepository.save(event);

        // Registrar medalla de inscripcion disponible (una por evento)
        badgeService.makeBadgeAvailable("evento-" + saved.getId() + "-primero");

        return ResponseEntity.ok(saved);
    }

    /**
     * POST /api/events/{id}/inscribir?userId=...
     * Inscribe a un usuario al evento y le otorga medalla por inscripcion.
     */
    @PostMapping("/{id}/inscribir")
    public ResponseEntity<Event> inscribir(@PathVariable String id,
                                           @RequestParam String userId) {
        return eventRepository.findById(id)
                .map(event -> {
                    if (!event.getAsistentes().contains(userId)) {
                        event.getAsistentes().add(userId);
                        eventRepository.save(event);

                        // Medalla automatica por inscribirse
                        badgeService.awardBadge(userId, "INSCRIPCION_EVENTO",
                                "Participante",
                                "Medalla por inscribirse al evento: " + event.getTitulo());
                    }
                    return ResponseEntity.ok(event);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
