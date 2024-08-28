package com.agenda.Class

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.agenda.R

/**
 * Adapter personalizzato per uno Spinner che include immagini e testo.
 * @param context Il contesto dell'applicazione.
 * @param options Un array di stringhe che rappresentano le opzioni dello Spinner.
 * @param images Un array opzionale di risorse immagine corrispondenti alle opzioni.
 */
class CustomSpinnerAdapter(
    private val context: Context,
    private val options: Array<String>,
    private val images: Array<Int>? = null
) : ArrayAdapter<String>(context, R.layout.spinner_item, options) {

    /**
     * Restituisce la vista per l'elemento selezionato nello Spinner.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    /**
     * Restituisce la vista per ogni elemento nel menu a discesa dello Spinner.
     */
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    /**
     * Crea e restituisce una vista personalizzata per l'elemento dello Spinner.
     * @param position La posizione dell'elemento nello Spinner.
     * @param convertView La vista riutilizzabile, se disponibile.
     * @param parent Il gruppo di vista padre a cui questa vista sarà eventualmente attaccata.
     * @return La vista personalizzata per l'elemento dello Spinner.
     */
    private fun getCustomView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Usa la vista riutilizzabile se disponibile, altrimenti infla una nuova vista
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false)

        // Trova le viste dell'immagine e del testo nel layout
        val imageView: ImageView? = view.findViewById(R.id.imageView)
        val textView: TextView = view.findViewById(R.id.textView)

        // Imposta l'immagine se presente
        if (images != null && position < images.size) {
            imageView?.setImageResource(images[position])
        }

        // Imposta il testo per l'opzione corrente
        textView.text = options[position]

        return view
    }
}