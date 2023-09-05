package org.example.controller;

import org.example.MicroClient;
import org.example.model.NoteDocument;
import org.example.model.NoteDocumentRefBinder;
import org.example.model.NoteException;
import org.example.model.NotePlain;
import org.example.service.NoteService;
import org.example.storage.NoteDocumentRepository;
import org.example.storage.NoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
public class NotesRefController {

    private static final Logger log =
            LoggerFactory.getLogger(NotesController.class);

    @ExceptionHandler
    public ResponseEntity<String> handle(ResponseStatusException rse) {
        return new ResponseEntity<String>(rse.getMessage(), rse.getStatusCode());
    }

    @Autowired
    NoteRepository noteRepository = null;
    @Autowired
    NoteDocumentRepository noteDocRepository = null;

    @Autowired
    NoteService noteService = null;

    @PostMapping("NoteDocReference")
    public void storeNoteWithDocumentReferences(@RequestBody NoteDocumentRefBinder noteBinder){
        ArrayList<String> docIds = null;
        NotePlain note = null;
        NotePlain persistentNote = null;

        docIds = noteBinder.getDocIds();

        String urlTeste = noteService.buildDocumentExistenceURL(docIds);

        MicroClient client = null;
        client = new MicroClient();
        client.init();
        String TestResult = client.getDataFromEndpoint(urlTeste);

        if(TestResult.equals("false")){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "One of the documents doesnt exists on the database");
        }

        try{
            note = noteBinder.getNote();
            persistentNote =
                    noteRepository.findNotePlainByNoteId(note.getNoteId());
            if(persistentNote == null || persistentNote.getNoteId().equals("")){
                noteRepository.save(note);
            }

            docIds = noteBinder.getDocIds();

            for(int i = 0; i < docIds.size(); i++){
                log.info("Saved note id " + note.getNoteId() + " " +
                                "and doc id: " + docIds.get(i));
                noteDocRepository.save(new NoteDocument(note.getNoteId(),
                        docIds.get(i)));
            }
            noteDocRepository.flush();

        } catch (Exception e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "An error ocurred saving document reference" +
                            " to a note " + e.getMessage());
        }
    }

    @GetMapping("NoteDocuments")
    public List<NoteDocument> getNoteDocs(){

        List<NoteDocument> noteDocs = null;

        try{
            noteDocs = noteDocRepository.findAll();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    e.getMessage());
        }
        return noteDocs;
    }

    @GetMapping("NoteDocReferences/{noteId}")
    public NoteDocumentRefBinder getNoteWithDocumentReferences(@PathVariable Long noteId){
        NoteDocumentRefBinder noteBinder = null;
        NotePlain note = null;
        NoteDocument[] noteDocuments = null;
        String docId = "";
        try{
            noteBinder = new NoteDocumentRefBinder();
            note = noteRepository.findNotePlainByNoteId(noteId);
            if(note == null || note.getNoteId().equals("")){
                throw new NoteException("The note did not exist in the " +
                        "database note id: " + noteId);
            }
            noteBinder.setNote(note);
            noteDocuments =
                    noteDocRepository.findAllNoteDocumentsByNoteId(noteId);
            for(int i = 0 ; i < noteDocuments.length; i++){
                docId = noteDocuments[i].getDocId();
                log.info("adding a document reference to the binder, docId: "+ docId + noteBinder);
                noteBinder.addDocumentReference(docId);
            }
        } catch (Exception e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "An error ocurred retrieving the note and note " +
                    "references for note id: " + noteId);
        }
        return noteBinder;
    }

    @GetMapping("NoteDocuments/{noteId}")
    public String GetNoteAndDocuments(@PathVariable Long noteId){
        NotePlain note = null;
        NoteDocument[] noteDocuments = null;
        String docId = "";
        MicroClient client = null;
        String docResult = "";

        try{
            note = noteRepository.findNotePlainByNoteId(noteId);
            if(note == null || note.getNoteId().equals("")){
                throw new NoteException("The note did not exist in the " +
                        "database note id: " + noteId);
            }

            client = new MicroClient();
            client.init();
            noteDocuments =
                    noteDocRepository.findAllNoteDocumentsByNoteId(noteId);
            for(int i = 0; i < noteDocuments.length; i++){
                docId = noteDocuments[i].getDocId();
                try {
                    docResult = client.getDataFromEndpoint("http://149.100.142.12/note-documents" + "/document/" + docId);
                    return("Result from the Doc Service: " + docResult);
                } catch (WebClientResponseException ex) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "HTTP Error: " + ex.getRawStatusCode());
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Error making GET request");
                }
            }
        } catch (Exception e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Error retrieving the note or doc info");
        }
        return "error";
    }


}
