package com.agenda.Utilities

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import java.util.regex.Pattern

/**
 * UserActivity è un oggetto singleton che gestisce le operazioni di autenticazione e gestione
 * degli utenti utilizzando Firebase Authentication. Include metodi per login, logout, registrazione
 * e validazione degli input.
 */
object UserActivity {

    // Istanza di FirebaseAuth per gestire le operazioni di autenticazione
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Ottiene l'utente corrente autenticato, se esiste.
     *
     * @return L'utente autenticato, oppure null se non c'è un utente autenticato.
     */
    @JvmStatic
    fun getCurrentUser() = try {
        mAuth.currentUser
    } catch (e: Exception) {
        null
    }

    /**
     * Effettua il login con email e password, e richiama il callback con il risultato.
     *
     * @param email L'email dell'utente.
     * @param password La password dell'utente.
     * @param callback Una funzione di callback che viene chiamata con il risultato dell'operazione di login.
     *                 - `Boolean` indica se il login è stato effettuato con successo.
     *                 - `String?` contiene un messaggio di errore in caso di fallimento.
     */
    @JvmStatic
    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        // Valida i dati di input per il login e ottiene un eventuale messaggio di errore
        val errorMessage = validateLoginInput(email, password)

        // Se i dati di input non sono validi, invoca il callback con l'errore e termina
        if (errorMessage != null) {
            callback.invoke(false, errorMessage)
            return
        }

        // Esegue il tentativo di login con Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Login riuscito, invoca il callback con successo
                    callback.invoke(true, null)
                } else {
                    // Login fallito, ottiene il messaggio di errore dalla task
                    val error = task.exception?.message ?: "Errore sconosciuto durante l'autenticazione"
                    // Invoca il callback con il fallimento e il messaggio di errore
                    callback.invoke(false, error)
                }
            }
    }

    /**
     * Effettua il logout dell'utente corrente.
     */
    @JvmStatic
    fun logout() {
        mAuth.signOut()
    }

    /**
     * Registra un nuovo utente con nome, cognome, email e password, e richiama il callback con il risultato.
     *
     * @param nome Il nome dell'utente.
     * @param cognome Il cognome dell'utente.
     * @param email L'email dell'utente.
     * @param password La password dell'utente.
     * @param confPassword La conferma della password.
     * @param callback Una funzione di callback che viene chiamata con il risultato dell'operazione di registrazione.
     *                 - `Boolean` indica se la registrazione è stata effettuata con successo.
     *                 - `String?` contiene un messaggio di errore in caso di fallimento.
     */
    @JvmStatic
    fun register(
        nome: String,
        cognome: String,
        email: String,
        password: String,
        confPassword: String,
        callback: (Boolean, String?) -> Unit
    ) {
        // Valida i dati di input per la registrazione e ottiene un eventuale messaggio di errore
        var errorMessage = validateRegistrationInput(nome, cognome, email, password, confPassword)
        if (errorMessage != null) {
            callback.invoke(false, errorMessage)
            return
        }

        // Esegue la registrazione del nuovo utente con Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    if (user != null) {
                        // Imposta il nome utente come "email: nome cognome"
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName("$email: $nome $cognome")
                            .build()
                        user.updateProfile(profileUpdates)
                        callback.invoke(true, "Registrazione avvenuta con successo: $email")
                    } else {
                        callback.invoke(false, "Errore durante il recupero dell'utente registrato")
                    }
                } else {
                    // Registrazione fallita, ottiene il messaggio di errore dalla task
                    errorMessage = task.exception?.message ?: "Errore sconosciuto durante l'autenticazione"
                    callback.invoke(false, errorMessage)
                }
            }
    }

    /**
     * Valida l'input per il login, restituendo un messaggio di errore se necessario.
     *
     * @param email L'email dell'utente.
     * @param password La password dell'utente.
     * @return Un messaggio di errore se l'input non è valido, altrimenti null.
     */
    private fun validateLoginInput(email: String, password: String): String? {
        return when {
            email.isEmpty() && password.isEmpty() -> "Email e password non inseriti."
            email.isEmpty() -> "Email non inserita."
            password.isEmpty() -> "Password non inserita."
            else -> null
        }
    }

    /**
     * Valida l'input per la registrazione, restituendo un messaggio di errore se necessario.
     *
     * @param nome Il nome dell'utente.
     * @param cognome Il cognome dell'utente.
     * @param email L'email dell'utente.
     * @param password La password dell'utente.
     * @param confPassword La conferma della password.
     * @return Un messaggio di errore se l'input non è valido, altrimenti null.
     */
    private fun validateRegistrationInput(
        nome: String,
        cognome: String,
        email: String,
        password: String,
        confPassword: String
    ): String? {

        // Controlla se l'email è valida
        fun isEmailValid(email: String): Boolean {
            val emailPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
            return Pattern.compile(emailPattern).matcher(email).matches()
        }

        // Controlla se la password è valida
        fun isValidPassword(password: String): Boolean {

            // Restituisce true solo se tutte le condizioni sono soddisfatte:
            // - La lunghezza della password è almeno di 8 caratteri
            // - Contiene almeno una lettera maiuscola
            // - Contiene almeno una lettera minuscola
            // - Contiene almeno un numero
            // - Contiene almeno un carattere speciale
            return password.length >= 8
                    && password.any { it.isUpperCase() }
                    && password.any { it.isLowerCase() }
                    && password.any { it.isDigit() }
                    && password.any { "!@#$%^&*()-_=+".contains(it) }
        }

        // Valida i dati di input per la registrazione e restituisce un eventuale messaggio di errore
        return when {
            nome.isEmpty() -> "Nome non inserito"
            cognome.isEmpty() -> "Cognome non inserito"
            email.isEmpty() -> "Email non inserita"
            !isEmailValid(email) -> "Email non valida"
            password.isEmpty() -> "Password non inserita"
            !isValidPassword(password) -> "Password non valida"
            password != confPassword -> "Le password non coincidono"
            else -> null
        }
    }
}