package org.example.controller;

import org.example.model.NotePlain;
import org.example.storage.NoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class NotesController {

    //simple logging facade
    private static final Logger log =
            LoggerFactory.getLogger(NotesController.class);

    @Autowired
    NoteRepository noteRepository = null;

    @ExceptionHandler
    public ResponseEntity<String> handle(ResponseStatusException rse) {
        return new ResponseEntity<String>(rse.getMessage(), rse.getStatusCode());
    }

    @PostMapping("note")
    public void storeNote(@RequestBody NotePlain note) {
        try {
            note = noteRepository.saveAndFlush(note);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    e.getMessage());
        }
    }
    @GetMapping("notes")
    public List<NotePlain> getNotes() {
        List<NotePlain> notes = null;
        try {
            notes = noteRepository.findAll();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    e.getMessage());
        }
        return notes;
    }

    @GetMapping("notes/{header}")
    public NotePlain[] getNotesByHeader(@PathVariable String header){
        NotePlain[] notes = null;
        try{
            notes = noteRepository.findNotesByHeader(header);

        } catch (Exception e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Error retrieving the notes with header: " + header);
        }
        return notes;
    }

    @GetMapping("notes/{header}/{body}")
    public NotePlain[] getNotesByHeaderAndBody(@PathVariable String header,
                                               @PathVariable String body){
        NotePlain[] notes = null;
        try{
            log.info("Exercise 4, retrieving notes with header: " + header);
            notes = noteRepository.findNotesByHeaderAndBody(header, body);
            log.info("Exercise 4, retrieved " + notes.length + " notes with " +
                    "header: " + header);

        } catch (Exception e){
            log.error("Exercise 4, error retrieving the notes from the " +
                    "database with header: " + header, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Error retrieving the notes with header: " + header);
        }
        return notes;
    }
}

