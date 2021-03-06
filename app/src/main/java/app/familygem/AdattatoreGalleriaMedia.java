// Adattatore per RecyclerView con lista dei media

package app.familygem;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.folg.gedcom.model.Media;
import java.util.Map;
import app.familygem.dettaglio.Immagine;

class AdattatoreGalleriaMedia extends RecyclerView.Adapter<AdattatoreGalleriaMedia.gestoreVistaMedia> {
	private Object[] listaMedia;
	private Object[] listaContenitori;
	private boolean dettagli;
	AdattatoreGalleriaMedia( Map<Media,Object> lista, boolean dettagli ) {
		listaMedia = lista.keySet().toArray();
		listaContenitori = lista.values().toArray();
		this.dettagli = dettagli;
	}
	@Override
	public AdattatoreGalleriaMedia.gestoreVistaMedia onCreateViewHolder( ViewGroup parent, int tipo ) {
		View vista = LayoutInflater.from(parent.getContext()).inflate( R.layout.pezzo_media, parent, false );
		return new AdattatoreGalleriaMedia.gestoreVistaMedia( vista, dettagli );
	}
	@Override
	public void onBindViewHolder( final AdattatoreGalleriaMedia.gestoreVistaMedia gestore, int posizione ) {
		if( gestore.media == null ) {
			gestore.setta( posizione );
		}
	}
	@Override
	public int getItemCount() {
		return listaMedia.length;
	}
	@Override
	public long getItemId(int position) {
		return position;
	}
	@Override
	public int getItemViewType(int position) {
		return position;
	}

	class gestoreVistaMedia extends RecyclerView.ViewHolder implements View.OnClickListener {
		View vista;
		boolean dettagli;
		Media media;
		Object contenitore;
		ImageView vistaImmagine;
		TextView vistaTesto;
		TextView vistaNumero;
		gestoreVistaMedia( View vista, boolean dettagli ) {
			super(vista);
			this.vista = vista;
			this.dettagli = dettagli;
			vistaImmagine = vista.findViewById( R.id.media_img );
			vistaTesto = vista.findViewById( R.id.media_testo );
			vistaNumero = vista.findViewById( R.id.media_num );
		}
		void setta( int posizione ) {
			media = (Media)listaMedia[posizione];
			contenitore = listaContenitori[posizione];
			if( dettagli ) {
				String testo = "";
				if( media.getTitle() != null )
					testo = media.getTitle() + "\n";
				if( Globale.preferenze.esperto && media.getFile() != null ) {
					String file = media.getFile();
					file = file.replace( '\\', '/' );
					if( file.lastIndexOf('/') > -1 ) {
						if( file.length() > 1 && file.endsWith("/") ) // rimuove l'ultima barra
							file = file.substring( 0, file.length()-1 );
						file = file.substring( file.lastIndexOf('/') + 1 );
					}
					testo += file;
				}
				if( testo.isEmpty() )
					vistaTesto.setVisibility( View.GONE );
				else {
					if( testo.endsWith("\n") )
						testo = testo.substring( 0, testo.length()-1 );
					vistaTesto.setText( testo );
				}
				if( media.getId() != null ) {
					vistaNumero.setText( String.valueOf(Galleria.popolarita(media)) );
				} else
					vistaNumero.setVisibility( View.GONE );
				vista.setOnClickListener( this );
				((Activity)vista.getContext()).registerForContextMenu( vista );
				vista.setTag( R.id.tag_oggetto, media );
				vista.setTag( R.id.tag_contenitore, contenitore );
				// Registra menu contestuale
				final AppCompatActivity attiva = (AppCompatActivity) vista.getContext();
				if( vista.getContext() instanceof Individuo ) { // Fragment individuoMedia
					attiva.getSupportFragmentManager()
							.findFragmentByTag( "android:switcher:" + R.id.schede_persona + ":0" )    // non garantito in futuro
							.registerForContextMenu( vista );
				} else if( vista.getContext() instanceof Principe ) // Fragment Galleria
					attiva.getSupportFragmentManager().findFragmentById( R.id.contenitore_fragment ).registerForContextMenu( vista );
				else    // nelle AppCompatActivity
					attiva.registerForContextMenu( vista );
			} else {
				//vistaImmagine.setMaxHeight( 110 ); // no
				RecyclerView.LayoutParams parami = new RecyclerView.LayoutParams( RecyclerView.LayoutParams.WRAP_CONTENT, 170 );
				parami.setMargins( 5, 5, 5, 5 );
				vista.setLayoutParams( parami );
				vistaTesto.setVisibility( View.GONE );
				vistaNumero.setVisibility( View.GONE );
			}
			//new U.MostraMedia( vistaImmagine, true ).execute( media );
			U.dipingiMedia( media, vistaImmagine, (ProgressBar)vista.findViewById(R.id.media_circolo) );
		}
		@Override
		public void onClick( View v ) {
			AppCompatActivity attiva = (AppCompatActivity) v.getContext();
			// Galleria in modalità scelta dell'oggetto media
			// Restituisce l'id di un oggetto media a IndividuoMedia
			if( attiva.getIntent().getBooleanExtra( "galleriaScegliMedia", false ) ) {
				Intent intent = new Intent();
				intent.putExtra( "idMedia", media.getId() );
				attiva.setResult( Activity.RESULT_OK, intent );
				attiva.finish();
			// Galleria in modalità normale apre Immagine
			} else {
				Ponte.manda( media, "oggetto" );
				Ponte.manda( contenitore, "contenitore" );
				v.getContext().startActivity( new Intent( v.getContext(), Immagine.class ) );
			}
		}
	}
}