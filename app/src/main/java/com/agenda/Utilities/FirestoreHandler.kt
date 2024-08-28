package com.agenda.Utilities

import com.agenda.Class.NoteIdentifier
import com.agendapersonale.src.Class.MarkerInfo
import com.agendapersonale.src.Class.Note
import com.agendapersonale.src.Class.NoteData
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

/**
 * FirestoreHandler è un oggetto singleton che gestisce le operazioni di Create, Read, Update, Delete
 * su note e relativi dati (date degli eventi e marker) utilizzando Firebase Firestore.
 */
object FirestoreHandler {

    // Costanti per i nomi delle collezioni e dei campi nel database Firestore
    private const val COLLECTION_NOTES = "notes"
    private const val SUBCOLLECTION_NOTE_DATES = "noteDates"
    private const val FIELD_EMAIL_UTENTE = "emailUtente"
    private const val FIELD_TITOLO = "titolo"
    private const val FIELD_DESCRIZIONE = "descrizione"
    private const val FIELD_EVENT_CATEGORY = "eventCategory"
    private const val FIELD_START_DATE = "startDate"
    private const val FIELD_END_DATE = "endDate"
    private const val FIELD_PREAVVISO = "preavviso"
    private const val FIELD_LATITUDE = "latitude"
    private const val FIELD_LONGITUDE = "longitude"
    private const val SUBCOLLECTION_MARKER_INFO = "marker_info"

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Aggiunge una nota con le relative date e informazioni sui marker al database Firestore.
     *
     * @param titolo Il titolo della nota.
     * @param descrizione La descrizione della nota (opzionale).
     * @param noteCategory La categoria della nota (opzionale).
     * @param markersInfo Le informazioni sui marker della nota (opzionale).
     * @param notesDates Le date associate alla nota.
     * @param callback Una funzione di callback che viene chiamata con il risultato dell'operazione.
     */
    @JvmStatic
    fun addNoteWithDates(
        titolo: String,
        descrizione: String?,
        noteCategory: String?,
        markersInfo: List<MarkerInfo>?,
        notesDates: List<NoteData>,
        callback: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        val email = UserActivity.getCurrentUser()?.email
        if (email == null) {
            callback(false, "Utente non autenticato")
            return
        }

        val notesCollection = firestore.collection(COLLECTION_NOTES)

        // Aggiunge una nuova nota al Firestore
        notesCollection.add(Note(
            titolo,
            email,
            descrizione,
            noteCategory
        )).addOnSuccessListener { documentReference ->
            val noteDocumentId = documentReference.id
            // Aggiunge le date della nota al Firestore
            addNoteDatesToFirestore(notesCollection, noteDocumentId, notesDates) { success, errorMessage ->
                if (success) {
                    if (markersInfo != null) {
                        // Aggiunge le informazioni sui marker se presenti
                        addMarkerInfoToFirestore(notesCollection, noteDocumentId, markersInfo, callback)
                    } else {
                        callback(true, null)
                    }
                } else {
                    callback(false, errorMessage)
                }
            }
        }
            .addOnFailureListener { e ->
                callback(false, "Errore durante l'inserimento della nota: ${e.message}")
            }
    }

    /**
     * Recupera tutte le note dell'utente corrente e le passa al callback.
     *
     * @param callback Una funzione di callback che viene chiamata con una lista di Triple contenente le note,
     * i relativi dati e gli identificatori.
     */
    @JvmStatic
    fun viewNote(callback: (List<Triple<NoteData, Note, NoteIdentifier>>) -> Unit) {
        val email = UserActivity.getCurrentUser()?.email

        if (email == null) {
            // Callback chiamato con una lista vuota se l'utente non è autenticato
            callback(emptyList())
            return
        }

        // Ottiene la collezione delle note filtrata per email dell'utente
        val notesCollection = firestore.collection(COLLECTION_NOTES)
            .whereEqualTo(FIELD_EMAIL_UTENTE, email)

        // Inizializza la lista dei risultati
        val resultList = mutableListOf<Triple<NoteData, Note, NoteIdentifier>>()

        // Recupera le note dal Firestore
        notesCollection.get()
            .addOnSuccessListener { documents ->
                // Lista delle attività pendenti per il recupero delle date
                val pendingTasks = mutableListOf<Task<QuerySnapshot>>()

                for (document in documents) {
                    val titolo = document.getString(FIELD_TITOLO)

                    if (titolo != null) {
                        // Recupera i campi descrizione e categoria evento
                        val descrizione = document.getString(FIELD_DESCRIZIONE) ?: ""
                        val eventCategory = document.getString(FIELD_EVENT_CATEGORY) ?: ""

                        // Recupera le date associate alla nota
                        val noteDatesRef = document.reference.collection(SUBCOLLECTION_NOTE_DATES)
                        val task = noteDatesRef.get().addOnSuccessListener { noteDateDocs ->
                            for (noteDateDoc in noteDateDocs) {
                                // Ottiene le date di inizio e fine e il preavviso
                                val startDate = noteDateDoc.getTimestamp(FIELD_START_DATE)
                                val endDate = noteDateDoc.getTimestamp(FIELD_END_DATE)
                                val preavviso = noteDateDoc.getLong(FIELD_PREAVVISO)?.toInt() ?: 0

                                if (startDate != null && endDate != null) {
                                    // Aggiunge la nota alla lista dei risultati con i relativi identificatori
                                    resultList.add(
                                        Triple(
                                            NoteData(startDate, endDate, preavviso),
                                            Note(
                                                titolo,
                                                email,
                                                descrizione,
                                                eventCategory
                                            ),
                                            NoteIdentifier(document.id, noteDateDoc.id)
                                        )
                                    )
                                }
                            }
                        }
                        // Aggiunge il task alla lista delle attività pendenti
                        pendingTasks.add(task)
                    }
                }

                // Attende il completamento di tutte le attività pendenti
                Tasks.whenAllComplete(pendingTasks)
                    .addOnSuccessListener {
                        // Ordina resultList in base alla data di inizio (startDate)
                        resultList.sortBy { it.first.startDate }

                        // Chiamata del callback con la lista ordinata di eventi
                        callback(resultList)
                    }
                    .addOnFailureListener {
                        // Se una delle attività fallisce, il callback viene chiamato con una lista vuota
                        callback(emptyList())
                    }
            }
            .addOnFailureListener {
                // Gestione dell'errore di recupero delle note dalla collezione principale
                callback(emptyList())
            }
    }

    /**
     * Recupera le informazioni sui marker di una specifica nota e le passa al callback.
     *
     * @param id L'ID della nota.
     * @param callback Una funzione di callback che viene chiamata con una lista di MarkerInfo.
     */
    @JvmStatic
    fun listInfoMarker(
        id: String,
        callback: (List<MarkerInfo>) -> Unit
    ) {

        val markersCollection = firestore.collection(COLLECTION_NOTES)
            .document(id)
            .collection(SUBCOLLECTION_MARKER_INFO)

        // Recupera le informazioni sui marker dal Firestore
        markersCollection.get()
            .addOnSuccessListener { markerDocs ->
                val markerList = mutableListOf<MarkerInfo>()

                for (markerDoc in markerDocs) {
                    val latitude = markerDoc.getDouble(FIELD_LATITUDE)
                    val longitude = markerDoc.getDouble(FIELD_LONGITUDE)

                    if (latitude != null && longitude != null) {
                        markerList.add(MarkerInfo(latitude, longitude))
                    }
                }

                // Chiamata del callback con la lista di MarkerInfo
                callback(markerList)
            }
            .addOnFailureListener { e ->
                // Gestisci eventuali errori e chiama il callback con una lista vuota
                callback(emptyList())
            }
    }

    /**
     * Elimina una nota e le sue relative date dal database Firestore.
     *
     * @param noteDocumentId L'ID del documento della nota.
     * @param noteDateId L'ID del documento della data della nota.
     * @param callback Una funzione di callback che viene chiamata con il risultato dell'operazione.
     */
    @JvmStatic
    fun deleteNote(noteDocumentId: String, noteDateId: String, callback: (Boolean) -> Unit) {
        val notesCollection = firestore.collection(COLLECTION_NOTES)
        val noteDatesCollection = notesCollection
            .document(noteDocumentId)
            .collection(SUBCOLLECTION_NOTE_DATES)

        // Elimina l'elemento dalla sottocollezione "noteDates"
        noteDatesCollection.document(noteDateId)
            .delete()
            .addOnSuccessListener {
                // Verifica se la sottocollezione è vuota dopo l'eliminazione
                noteDatesCollection.get()
                    .addOnSuccessListener { documents ->
                        // Se non ci sono altri documenti nella sottocollezione, elimina l'evento principale
                        if (documents.isEmpty) {
                            notesCollection.document(noteDocumentId)
                                .delete()
                                .addOnSuccessListener {
                                    // L'evento principale è stato eliminato con successo
                                    callback(true)
                                }
                                .addOnFailureListener { e ->
                                    // Gestione degli errori durante l'eliminazione dell'evento principale
                                    callback(false)
                                }
                        } else {
                            // L'eliminazione dell'elemento dalla sottocollezione è avvenuta con successo
                            callback(true)
                        }
                    }
                    .addOnFailureListener { e ->
                        // Gestione degli errori durante il recupero dei documenti dalla sottocollezione
                        callback(false)
                    }
            }
            .addOnFailureListener { e ->
                // Gestione degli errori durante l'eliminazione dell'elemento dalla sottocollezione "noteDates"
                callback(false)
            }
    }

    /**
     * Aggiunge le date di una nota al database Firestore.
     *
     * @param notesCollection La collezione delle note.
     * @param noteDocumentId L'ID del documento della nota.
     * @param noteDates Le date della nota.
     * @param callback Una funzione di callback che viene chiamata con il risultato dell'operazione.
     */
    private fun addNoteDatesToFirestore(
        notesCollection: CollectionReference,
        noteDocumentId: String,
        noteDates: List<NoteData>,
        callback: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        // Ottiene la collezione delle date della nota specifica
        val noteDatesCollection = notesCollection
            .document(noteDocumentId)
            .collection(SUBCOLLECTION_NOTE_DATES)

        // Crea un batch per eseguire operazioni atomiche su Firestore
        val batch = firestore.batch()

        // Aggiunge ogni data della nota al batch
        noteDates.forEach { noteData ->
            batch.set(noteDatesCollection.document(), noteData)
        }

        // Esegue il commit del batch
        batch.commit()
            .addOnSuccessListener {
                // Chiamata del callback con successo se il commit è riuscito
                callback(true, null)
            }
            .addOnFailureListener { e ->
                // Chiamata del callback con errore se il commit fallisce
                callback(false, "Errore durante l'inserimento delle date della nota: ${e.message}")
            }
    }

    /**
     * Aggiunge le informazioni sui marker al database Firestore.
     *
     * @param notesCollection La collezione delle note.
     * @param noteDocumentId L'ID del documento della nota.
     * @param markersInfo Le informazioni sui marker.
     * @param callback Una funzione di callback che viene chiamata con il risultato dell'operazione.
     */
    private fun addMarkerInfoToFirestore(
        notesCollection: CollectionReference,
        noteDocumentId: String,
        markersInfo: List<MarkerInfo>,
        callback: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        // Ottiene la collezione dei marker associata alla nota specifica
        val markerInfoCollection = notesCollection
            .document(noteDocumentId)
            .collection(SUBCOLLECTION_MARKER_INFO)

        // Crea un batch per eseguire operazioni atomiche su Firestore
        val batch = FirebaseFirestore.getInstance().batch()

        // Aggiunge ogni marker alla collezione come parte del batch
        markersInfo.forEach { marker ->
            batch.set(markerInfoCollection.document(), marker)
        }

        // Esegue il commit del batch per salvare tutte le modifiche
        batch.commit()
            .addOnSuccessListener {
                // Chiamata del callback con successo se il commit è riuscito
                callback(true, null)
            }
            .addOnFailureListener { e ->
                // Chiamata del callback con errore se il commit fallisce
                callback(false, "Errore durante l'inserimento delle informazioni dei marker: ${e.message}")
            }
    }
}