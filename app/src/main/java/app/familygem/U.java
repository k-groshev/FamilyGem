// Attrezzi utili per tutto il programma
package app.familygem;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.JsonPrimitive;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.folg.gedcom.model.Change;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.ExtensionContainer;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GedcomTag;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.MediaContainer;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.NoteContainer;
import org.folg.gedcom.model.NoteRef;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.Source;
import org.folg.gedcom.model.SourceCitation;
import org.folg.gedcom.model.SourceCitationContainer;
import org.folg.gedcom.parser.JsonParser;
import org.joda.time.Months;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormat;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import app.familygem.dettaglio.CitazioneFonte;
import app.familygem.dettaglio.Fonte;
import app.familygem.dettaglio.Immagine;
import app.familygem.dettaglio.Nota;
import app.familygem.visita.ListaMedia;
import app.familygem.visita.RiferimentiNota;

public class U {
	
	// restituisce l'id della Person iniziale di un Gedcom
	public static String trovaRadice( Gedcom gc ) {
		if( gc.getHeader() != null)
			if( valoreTag( gc.getHeader().getExtensions(), "_ROOT" ) != null )
				return valoreTag( gc.getHeader().getExtensions(), "_ROOT" );
		if( !gc.getPeople().isEmpty() )
				return gc.getPeople().get(0).getId();
		return null;
	}
	
	// riceve una Person e restituisce stringa con nome e cognome principale
	static String epiteto( Person p ) {
		if( !p.getNames().isEmpty() )
			return nomeCognome( p.getNames().get(0) );
		return "";
	}

	// riceve una Person e restituisce il titolo nobiliare
	static String titolo( Person p ) {
		// Gedcom standard INDI.TITL
		for( EventFact ef : p.getEventsFacts() )
			if( ef.getTag() != null )
				if( ef.getTag().equals( "TITL" ) )
					return ef.getValue();
		// Così invece prende INDI.NAME._TYPE.TITL, vecchio metodo di org.folg.gedcom
		for( Name n : p.getNames() )
			if( n.getType() != null )
				if( n.getType().equals( "TITL" ) )
					return  n.getValue();
		return "";
	}

	// Restituisce il nome e cognome addobbato di un Name
	public static String nomeCognome( Name n ) {
		String completo = "";
		String grezzo = n.getValue().trim();
		if( grezzo.indexOf('/') > -1 ) // Se c'è un cognome tra '/'
			completo = grezzo.substring( 0, grezzo.indexOf('/') ).trim(); // nome
		if (n.getNickname() != null)
			completo += " \"" + n.getNickname() + "\"";
		if( grezzo.indexOf('/') < grezzo.lastIndexOf('/') ) {
			completo += " " + grezzo.substring( grezzo.indexOf('/') + 1, grezzo.lastIndexOf('/') ).trim(); // cognome
		}
		if( grezzo.length() - 1 > grezzo.lastIndexOf('/') )
			completo += " " + grezzo.substring( grezzo.lastIndexOf('/') + 1 ).trim(); // dopo il cognome
		if( n.getPrefix() != null )
			completo = n.getPrefix().trim() + " " + completo;
		if( n.getSuffix() != null )
			completo += " " + n.getSuffix().trim();
		return completo.trim();
	}

	// Restituisce il cognome di una persona
	public static String cognome( Person p ) {
		String cognome = "";
		if( !p.getNames().isEmpty() ) {
			String grezzo = p.getNames().get(0).getValue();
			if( grezzo.indexOf('/') < grezzo.lastIndexOf('/') ) {
				cognome = grezzo.substring( grezzo.indexOf('/') + 1, grezzo.lastIndexOf('/') ).trim();
			}
		}
		return cognome;
	}

	// Riceve una Person e restituisce il sesso: 0 senza SEX, 1 Maschio, 2 Femmina, 3 Undefinito, 4 altro
	public static int sesso( Person p ) {
		for( EventFact fatto : p.getEventsFacts() ) {
			if( fatto.getTag()!=null && fatto.getTag().equals("SEX") ) {
				if( fatto.getValue() == null )
					return 4;  // c'è 'SEX' ma il valore è vuoto
				else {
					switch( fatto.getValue() ) {
						case "M": return 1;
						case "F": return 2;
						case "U": return 3;
						default: return 4; // altro valore
					}
				}
			}
		}
		return 0; // SEX non c'è
	}

	// Riceve una person e trova se è morto o seppellito
	static boolean morto( Person p ) {
		for( EventFact fatto : p.getEventsFacts() ) {
			if( fatto.getTag().equals( "DEAT" ) || fatto.getTag().equals( "BURI" ) )
				//s.l( p.getNames().get(0).getDisplayValue() +" > "+ fatto.getDisplayType() +": '"+ fatto.getValue() +"'" );
				//if( fatto.getValue().equals("Y") )
				return true;
		}
		return false;
	}

	// riceve una Person e restituisce una stringa con gli anni di nascita e morte 
	static String dueAnni( Person p, boolean conEta ) {
		String anni = "";
		LocalDate nascita = null,
				fine = null;
		for( EventFact unFatto : p.getEventsFacts() ) {
			if( unFatto.getTag() != null && unFatto.getTag().equals("BIRT") && unFatto.getDate() != null ) {
				anni = soloAnno( unFatto.getDate() );
				nascita = data( unFatto.getDate() );
				break;
			}
		}
		for( EventFact unFatto : p.getEventsFacts() ) {
			if( unFatto.getTag() != null && unFatto.getTag().equals("DEAT") && unFatto.getDate() != null ) {
				if( !anni.isEmpty() )
					anni += " – ";
				anni += soloAnno( unFatto.getDate() );
				fine = data( unFatto.getDate() );
				break;
			}
		}
		if( conEta && nascita != null ) {
			if( fine == null && nascita.isBefore(LocalDate.now()) && Years.yearsBetween(nascita,LocalDate.now()).getYears() < 100 ) {
				fine = LocalDate.now();
			}
			if( fine != null ) {
				String misura = "";
				int eta = Years.yearsBetween( nascita, fine ).getYears();
				if( eta < 2 ) {
					eta = Months.monthsBetween( nascita, fine ).getMonths();
					misura = " mesi";
					if( eta < 2 ) {
						eta = Days.daysBetween( nascita, fine ).getDays();
						misura = " giorni";
					}
				}
				if( eta >= 0 )
					anni += "  (" + eta + misura + ")";
				else
					anni += "  (?)";
			}

		}
		return anni;
	}

	// riceve una data in stile gedcom e restituisce l'annno semplificato alla mia maniera
	static String soloAnno( String data ) {
		String anno = data.substring( data.lastIndexOf(" ")+1 );	// prende l'anno che sta in fondo alla data
		if( anno.contains("/") )	// gli anni tipo '1711/12' vengono semplificati in '1712'
			anno = anno.substring(0,2).concat( anno.substring(anno.length()-2,anno.length()) );
		if( data.startsWith("ABT") || data.startsWith("EST") || data.startsWith("CAL") )
			anno = anno.concat("?");
		if( data.startsWith("BEF") )
			anno = "←".concat(anno);
		if( data.startsWith("AFT") )
			anno = anno.concat("→");
		if( data.startsWith("BET") ) {
			int pos = data.indexOf("AND") - 1;
			String annoPrima = data.substring( data.lastIndexOf(" ",pos-1)+1, pos );	// prende l'anno che sta prima di 'AND'
			if( !annoPrima.equals(anno) && anno.length()>3 ) {
				//s.l( annoPrima +"  " + anno );
				if( annoPrima.substring(0,2).equals(anno.substring(0,2)) )		// se sono dello stesso secolo
					anno = anno.substring( anno.length()-2, anno.length() );	// prende solo gli anni
				anno = annoPrima.concat("~").concat(anno);
			}
		}
		return anno;
	}

	// Riceve una data stringa Gedcom e restituisce una singola LocalDate joda oppure null
	static LocalDate data( String dataGc ) {
		if( dataGc.contains("BEF") || dataGc.contains("AFT") || dataGc.contains("BET")
				|| dataGc.contains("FROM") || dataGc.contains("TO") )	// date incalcolabili
			return null;
		if( dataGc.contains("ABT") || dataGc.contains("EST") || dataGc.contains("CAL") || dataGc.contains("INT") )  // rimuove i tre porcellini
			dataGc = dataGc.substring( dataGc.indexOf(' ')+1, dataGc.length() );
		if( dataGc.contains("(") && dataGc.contains(")") )
			dataGc = dataGc.replaceAll("\\(.*?\\)", "").trim();
		if( dataGc.isEmpty() ) return null;
		String annoStr = dataGc.substring( dataGc.lastIndexOf(" ") + 1 );
		if( annoStr.contains("/") )	// gli anni tipo '1711/12' o '1711/1712' vengono semplificati in '1712'
			annoStr = annoStr.substring(0,2).concat( annoStr.substring(annoStr.length()-2,annoStr.length()) ); // TODO: E l'anno 1799/00 diventa 1700
		int anno = Anagrafe.idNumerico( annoStr );
		if( anno == 0 ) return null;
		int mese = 1;
		if( dataGc.length() > 4 && dataGc.indexOf(' ') > 0 ) {
			try {
				String meseStr = dataGc.substring( dataGc.lastIndexOf( ' ' ) - 3, dataGc.lastIndexOf( ' ' ) );
				mese = DateTimeFormat.forPattern( "MMM" ).withLocale( Locale.ENGLISH ).parseDateTime( meseStr ).getMonthOfYear();
			} catch( Exception e ) { return null; }
		}
		int giorno = 1;
		if( dataGc.length() > 8 ) {
			if( dataGc.indexOf(' ') > 0 )
				giorno = Anagrafe.idNumerico( dataGc.substring( 0, dataGc.indexOf(' ') ) );    // estrae i soli numeri
			if( giorno < 1 || giorno > 31)
				giorno = 1;
		}
		LocalDate data = null;
		try {
			data = new LocalDate( anno, mese, giorno ); // ad esempio '29 febbraio 1635' dà errore
		} catch( Exception e ) {
			e.printStackTrace();
		}
		return data;
	}

	// Restituisce la lista di estensioni
	@SuppressWarnings("unchecked")
	public static List<Estensione> trovaEstensioni( ExtensionContainer contenitore ) {
		if( contenitore.getExtension( "folg.more_tags" ) != null ) {
			List<Estensione> lista = new ArrayList<>();
			for( GedcomTag est : (List<GedcomTag>)contenitore.getExtension("folg.more_tags") ) {
				String testo = scavaEstensione(est);
				if( testo.endsWith("\n") )
					testo = testo.substring( 0, testo.length()-1 );
				lista.add( new Estensione( est.getTag(), testo, est ) );
			}
			return lista;
		}
		return Collections.emptyList();
	}

	// Costruisce un testo con il contenuto ricorsivo dell'estensione
	public static String scavaEstensione( GedcomTag pacco ) {
		String testo = "";
		//testo += pacco.getTag() +": ";
		if( pacco.getValue() != null )
			testo += pacco.getValue() +"\n";
		else if( pacco.getId() != null )
			testo += pacco.getId() +"\n";
		else if( pacco.getRef() != null )
			testo += pacco.getRef() +"\n";
		for( GedcomTag unPezzo : pacco.getChildren() )
			testo += scavaEstensione( unPezzo );
		return testo;
	}

	public static void eliminaEstensione( GedcomTag estensione, Object contenitore, View vista ) {
		if( contenitore instanceof ExtensionContainer ) { // IndividuoEventi
			@SuppressWarnings("unchecked")
			List<GedcomTag> lista = (List<GedcomTag>) ((ExtensionContainer)contenitore).getExtension( "folg.more_tags" );
			lista.remove( estensione );
		} else if( contenitore instanceof GedcomTag ) { // Dettaglio
			((GedcomTag)contenitore).getChildren().remove( estensione );
		}
		if( vista != null )
			vista.setVisibility( View.GONE );
	}

	// Restituisce il valore di un determinato tag in una estensione (GedcomTag)
	@SuppressWarnings("unchecked")
	static String valoreTag( Map<String,Object> mappaEstensioni, String nomeTag ) {
		for( Map.Entry<String,Object> estensione : mappaEstensioni.entrySet() ) {
			List<GedcomTag> listaTag = (ArrayList<GedcomTag>) estensione.getValue();
			for( GedcomTag unPezzo : listaTag ) {
				//l( unPezzo.getTag() +" "+ unPezzo.getValue() );
				if( unPezzo.getTag().equals( nomeTag ) ) {
					if( unPezzo.getId() != null )
						return unPezzo.getId();
					else if( unPezzo.getRef() != null )
						return unPezzo.getRef();
					else
						return unPezzo.getValue();
				}
			}
		}
		return null;
	}

	/* Aggiorna il REF di un tag nelle estensioni di un oggetto:  tag:"_ROOT"  ref:"I123"
	@SuppressWarnings("unchecked")
	static void aggiornaTag( Object obj, String nomeTag, String ref ) {
		String chiave = "gedcomy_tags";
		List<GedcomTag> listaTag = new ArrayList<>();
		boolean aggiungi = true;
		Map<String,Object> mappaEstensioni = ((ExtensionContainer) obj).getExtensions();	// ok
		if( !mappaEstensioni.isEmpty() ) {
			chiave = (String) mappaEstensioni.keySet().toArray()[0];	// chiave = 'folg.more_tags'
			listaTag = (ArrayList<GedcomTag>) mappaEstensioni.get( chiave );
			// Aggiorna tag esistente
			for( GedcomTag gct : listaTag ) {
				if( gct.getTag().equals(nomeTag) ) {
					gct.setRef( ref );
					aggiungi = false;
				}
			}
		}
		// Aggiunge nuovo tag
		if( aggiungi ) {
			GedcomTag tag = new GedcomTag( null, nomeTag, null );
			tag.setValue( ref );
			listaTag.add( tag );
		}
		((ExtensionContainer) obj).putExtension( chiave, listaTag );
	}*/


	// Riceve un Uri e cerca di restituire il percorso del file
	static String uriPercorsoFile( Uri uri ) {
		if( uri == null ) return null;
		if( uri.getScheme().equalsIgnoreCase( "file" )) {
			// file:///storage/emulated/0/DCIM/Camera/Simpsons.ged	  da File Manager
			// file:///storage/emulated/0/Android/data/com.dropbox.android/files/u1114176864/scratch/Simpsons.ged
			return uri.getPath();	// gli toglie  file://
		}
		String cosaCercare = OpenableColumns.DISPLAY_NAME;
		// Uri is different in versions after KITKAT (Android 4.4), we need to deal with different Uris
		//s.l( "uri Authority = " + uri.getAuthority() );
		//s.l( "isDocumentUri = " + DocumentsContract.isDocumentUri( Globale.contesto, uri) );	// false solo in G.Drive legacy
		// content://com.google.android.apps.docs.storage.legacy/enc%3DAPsNYqUd_MITZZJxxda1wvQP2ojY7f9xQCAPJoePEFIgSa-5%0A
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT 	// 21 sul mio cellu e 19
				&& DocumentsContract.isDocumentUri( Globale.contesto, uri )	// sempre true tranne con Google drive legacy
				) {
			//s.l( "Document Id = " + DocumentsContract.getDocumentId(uri) );
			switch( uri.getAuthority() ) {
				case "lab.gedcomy.localstorage.documents":	// ????? da testare
					return DocumentsContract.getDocumentId( uri );
				case "com.android.externalstorage.documents":	// memoria interna e scheda SD
					final String docId = DocumentsContract.getDocumentId(uri);
					// semplicemente prende l'ultima parte dell'uri
					// ad esempio 'primary:DCIM/Camera/Simpsons.ged'
					// oppure '3132-6232:famiglia/a_famigliotta_250.ged'
					final String[] split = docId.split(":");

					if( split[0].equalsIgnoreCase("primary")) {
						return Environment.getExternalStorageDirectory() + "/" + split[1];
						// Environment.getExternalStorageDirectory() restituisce sempre /storage/emulated/0 anche per la sd card
					} else {
						File[] luoghi = Globale.contesto.getExternalFilesDirs(null);
						for( File luogo : luoghi ) {
							if( luogo.getAbsolutePath().indexOf("/Android") > 0 ) {
								String dir = luogo.getAbsolutePath().substring(0, luogo.getAbsolutePath().indexOf("/Android"));
								File trovando = new File(dir, split[1]);
								// potrebbe capitare che in due schede SD c'è lo stesso percorso e nome file
								// l'utente sceglie il secondo e gli arriva il primo.
								if( trovando.exists() )
									return trovando.getAbsolutePath();
							}
						}
					}
					break;
				case "com.android.providers.downloads.documents":	// file dalla cartella Download
					/* Gli arriva un uri tipo	content://com.android.providers.downloads.documents/document/2326
					   e lo ricostruisce tipo	content://downloads/public_downloads/2326
					   così il cursor può interpretarlo anziché restituire null    */
					final String id = DocumentsContract.getDocumentId(uri);	// un numero tipo '2326'
					uri = ContentUris.withAppendedId( Uri.parse("content://downloads/public_downloads"), Long.valueOf(id) );
					cosaCercare = MediaStore.Files.FileColumns.DATA;
					//s.l( "uri ricostruito = " + uri );
				/*case "com.google.android.apps.docs.storage":	// Google drive 1
					// com.google.android.apps.docs.storage.legacy
				}*/
			}
		}
		String nomeFile = trovaNomeFile( uri, cosaCercare );
		if( nomeFile == null )
			nomeFile = trovaNomeFile( uri, OpenableColumns.DISPLAY_NAME );
		return nomeFile;
	}

	// Di default restituisce solo il nome del file 'famiglia.ged'
	// se il file è preso in downloads.documents restituisce il percorso completo
	private static String trovaNomeFile( Uri uri, String cosaCercare ) {
		String[] projection = { cosaCercare };
		Cursor cursore = Globale.contesto.getContentResolver().query( uri, projection, null, null, null);
		if( cursore != null && cursore.moveToFirst() ) {
			String nomeFile = cursore.getString( 0 );
			cursore.close();
			return nomeFile;
		}
		return null;
	}

	// Metodi per mostrare immagini:

	// Riceve una Person e sceglie il Media principale da cui ricavare l'immagine
	static void unaFoto( Person p, ImageView img ) {
		boolean trovatoQualcosa = false;
		for( Media med : p.getAllMedia(Globale.gc) ) {	// Cerca un media contrassegnato Primario Y
			if( med.getPrimary() != null && med.getPrimary().equals("Y") ) {
				dipingiMedia( med, img, null );
				trovatoQualcosa = true;
				break;
			}
		}
		if( !trovatoQualcosa )	// In alternativa restituisce il primo che trova
			for( Media med : p.getAllMedia(Globale.gc) ) {
				dipingiMedia( med, img, null );
				trovatoQualcosa = true;
				break;
			}
		if( !trovatoQualcosa )
			img.setVisibility( View.GONE );
	}

	/* ELIMINABILE
	fa comparire l'immagine in una vista immagine
	public static void mostraMedia( final ImageView vista, final Media med ) {
		vista.getViewTreeObserver().addOnPreDrawListener( new ViewTreeObserver.OnPreDrawListener() {
			public boolean onPreDraw() {
				vista.getViewTreeObserver().removeOnPreDrawListener(this);	// evita il ripetersi di questo metodo
				String percorso = percorsoMedia( med );	// Il file in locale
				if( percorso != null ) {
					BitmapFactory.Options opzioni = new BitmapFactory.Options();
					opzioni.inJustDecodeBounds = true;	// solo info
					BitmapFactory.decodeFile( percorso, opzioni );
					int largaOriginale = opzioni.outWidth;
					if( largaOriginale > vista.getWidth() && vista.getWidth() > 0 )
						opzioni.inSampleSize = largaOriginale / vista.getWidth();
					opzioni.inJustDecodeBounds = false;	// carica immagine
					Bitmap bitmap = BitmapFactory.decodeFile( percorso, opzioni );	// Riesce a ricavare un'immagine
					//bitmap = ThumbnailUtils.extractThumbnail( bitmap, 30, 60, ThumbnailUtils.OPTIONS_RECYCLE_INPUT );
					// Fico ma ritaglia l'immagine per farla stare nelle dimensioni date. La quarta opzione non l'ho capita
					try { // Rotazione Exif
						ExifInterface exif = new ExifInterface( percorso );
						int girata = exif.getAttributeInt( ExifInterface.TAG_ORIENTATION, 1 );
						int gradi = 0;
						switch( girata ) {
							//case ExifInterface.ORIENTATION_NORMAL:
							case ExifInterface.ORIENTATION_ROTATE_90:
								gradi = 90;
								break;
							case ExifInterface.ORIENTATION_ROTATE_180:
								gradi = 180;
								break;
							case ExifInterface.ORIENTATION_ROTATE_270:
								gradi = 270;
						}
						if( gradi > 0 ) {
							Matrix matrix = new Matrix();
							matrix.postRotate( gradi );
							bitmap = Bitmap.createBitmap( bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true );
						}
					} catch( Exception e ) {
						Toast.makeText( vista.getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
					}
					if( bitmap == null 	// Il file esiste in locale ma senza un'immagine
							|| ( bitmap.getWidth()<10 && bitmap.getHeight()>200 ) ) {	// Giusto per gli strambi mpg e mov
						// Magari è un video
						bitmap = ThumbnailUtils.createVideoThumbnail( percorso,	MediaStore.Video.Thumbnails.MINI_KIND );
						if( bitmap == null ) {
							String formato = med.getFormat();
							if( formato == null )
								formato = MimeTypeMap.getFileExtensionFromUrl( percorso );
							bitmap = generaIcona( vista, R.layout.media_file, formato );
						}
					}
					vista.setTag( R.id.tag_percorso, percorso );    // usato da Immagine.java
					vista.setImageBitmap( bitmap );
				} else if( med.getFile() != null )	// Cerca il file in internet
					new U.ZuppaMedia( vista, null, med ).execute( med.getFile() );
				return true;
			}
		});
	}*/

	/* Abbastanza buono ma Picasso sembra meglio
	Caricatore asincrono di immagini locali, sostiusce il metodo mostraMedia()
	static class MostraMedia extends AsyncTask<Media,Void,Bitmap> {
		private ImageView vista;
		boolean galleria;
		MostraMedia( ImageView vista, boolean galleria ) {
			this.vista = vista;
			this.galleria = galleria;
		}
		@Override
		protected Bitmap doInBackground( Media... params) {
			Media media = params[0];
			String percorso = U.percorsoMedia( media );
			Bitmap bitmap = null;
			if( percorso != null ) {
				BitmapFactory.Options opzioni = new BitmapFactory.Options();
				opzioni.inJustDecodeBounds = true;	// solo info
				BitmapFactory.decodeFile( percorso, opzioni );
				int largaOriginale = opzioni.outWidth;
				if( largaOriginale > vista.getWidth() && vista.getWidth() > 0 )
					opzioni.inSampleSize = largaOriginale / vista.getWidth();
				else if( largaOriginale > 300 ) // 300 una larghezza media approssimativa per una ImageView
					opzioni.inSampleSize = largaOriginale / 300;
				opzioni.inJustDecodeBounds = false;	// carica immagine
				bitmap = BitmapFactory.decodeFile( percorso, opzioni );	// Ricava un'immagine ridimensionata
				try { // Rotazione Exif
					ExifInterface exif = new ExifInterface( percorso );
					int girata = exif.getAttributeInt( ExifInterface.TAG_ORIENTATION, 1 );
					int gradi = 0;
					switch( girata ) {
						case ExifInterface.ORIENTATION_ROTATE_90:
							gradi = 90;
							break;
						case ExifInterface.ORIENTATION_ROTATE_180:
							gradi = 180;
							break;
						case ExifInterface.ORIENTATION_ROTATE_270:
							gradi = 270;
					}
					if( gradi > 0 ) {
						Matrix matrix = new Matrix();
						matrix.postRotate( gradi );
						bitmap = Bitmap.createBitmap( bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true );
					}
				} catch( Exception e ) {
					Toast.makeText( vista.getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
				}
				if( bitmap == null 	// Il file esiste in locale ma senza un'immagine
						|| ( bitmap.getWidth()<10 && bitmap.getHeight()>200 ) ) {	// Giusto per gli strambi mpg e mov
					// Magari è un video
					bitmap = ThumbnailUtils.createVideoThumbnail( percorso,	MediaStore.Video.Thumbnails.MINI_KIND );
					if( bitmap == null ) {
						String formato = media.getFormat();
						if( formato == null )
							formato = MimeTypeMap.getFileExtensionFromUrl( percorso );
						bitmap = U.generaIcona( vista, R.layout.media_file, formato );
					}
				}
				vista.setTag( R.id.tag_percorso, percorso );    // usato da Immagine.java
			} else if( media.getFile() != null )	// Cerca il file in internet
				new ZuppaMedia( vista, (ProgressBar)vista.findViewById(R.id.media_circolo), media ).execute( media.getFile() );
			// TODO ok ma un file in attesa di essere scaricato da internet blocca tutti gli altri anche locali
			return bitmap;
		}
		@Override
		protected void onPostExecute( Bitmap bitmap ) {
			if( bitmap != null ) {
				vista.setImageBitmap( bitmap );
				// Icona di file senza anteprima
				if( vista.getTag(R.id.tag_tipo_file).equals(3) ) {
					vista.setScaleType( ImageView.ScaleType.FIT_CENTER );
					if( galleria ) {
						RelativeLayout.LayoutParams parami = new RelativeLayout.LayoutParams(
								RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT );
						parami.addRule( RelativeLayout.ABOVE, R.id.media_testo );
						vista.setLayoutParams( parami );
					}
				}
			}
		}
	}*/

	// Mostra le immagini con il tanto declamato Picasso
	public static void dipingiMedia( final Media media, final ImageView vistaImmagine, final ProgressBar circo ) {
		final String percorso = U.percorsoMedia(media);
		if( circo!=null ) circo.setVisibility( View.VISIBLE );
		vistaImmagine.setTag( R.id.tag_tipo_file, 0 );
		if( percorso != null ) {
			Picasso.get().load( new File(percorso) )
					.placeholder( R.drawable.manichino )
					.fit() //.centerCrop()
					.centerInside()
					.into( vistaImmagine, new Callback() {
						@Override
						public void onSuccess() {
							if( circo!=null ) circo.setVisibility( View.GONE );
							vistaImmagine.setTag( R.id.tag_tipo_file, 1 );
							vistaImmagine.setTag( R.id.tag_percorso, percorso );
						}
						@Override
						public void onError( Exception e ) {
							// Magari è un video
							Bitmap bitmap = ThumbnailUtils.createVideoThumbnail( percorso,	MediaStore.Video.Thumbnails.MINI_KIND );
							vistaImmagine.setTag( R.id.tag_tipo_file, 2 );
							if( bitmap == null ) {
								// un File locale senza anteprima
								String formato = media.getFormat();
								if( formato == null )
									formato = MimeTypeMap.getFileExtensionFromUrl( percorso );
								bitmap = U.generaIcona( vistaImmagine, R.layout.media_file, formato );
								vistaImmagine.setScaleType( ImageView.ScaleType.FIT_CENTER );
								if( vistaImmagine.getParent() instanceof RelativeLayout && // brutto ma efficace
									((RelativeLayout)vistaImmagine.getParent()).findViewById(R.id.media_testo) != null ) {
									RelativeLayout.LayoutParams parami = new RelativeLayout.LayoutParams(
											RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT );
									parami.addRule( RelativeLayout.ABOVE, R.id.media_testo );
									vistaImmagine.setLayoutParams( parami );
								}
								vistaImmagine.setTag( R.id.tag_tipo_file, 3 );
							}
							vistaImmagine.setImageBitmap( bitmap );
							vistaImmagine.setTag( R.id.tag_percorso, percorso );
							if( circo!=null ) circo.setVisibility( View.GONE );
						}
					});
		} else if( media.getFile() != null ) { // magari è un'immagine in internet
			final String percorsoFile = media.getFile();
			Picasso.get().load(percorsoFile).fit()
					//.centerCrop()
					.placeholder( R.drawable.manichino ).centerInside()
					.into(vistaImmagine, new Callback() {
						@Override
						public void onSuccess() {
							if( circo!=null ) circo.setVisibility( View.GONE );
							vistaImmagine.setTag( R.id.tag_tipo_file, 1 );
							try {
								new ImboscaImmagine(media).execute(new URL(percorsoFile));
							} catch( Exception e ) {
								e.printStackTrace();
							}
						}
						@Override
						public void onError( Exception e ) {
							// Proviamo con una pagina web
							new ZuppaMedia( vistaImmagine, circo, media ).execute( percorsoFile );
						}
					});
		} else {
			if( circo!=null ) circo.setVisibility( View.GONE );
			vistaImmagine.setImageResource( R.drawable.manichino );
			vistaImmagine.setAlpha( 0.5f );
		}
	}

	// Riceve un Media, cerca il file in locale con diverse combinazioni di percorso e restituisce l'indirizzo
	static String percorsoMedia( Media m ) {
		Globale.preferenze.traghetta(); // todo questo traghettatore poi se ne potrà andare
		if( m.getFile() != null ) {
			String nome = m.getFile().replace("\\", "/");
			// Percorso FILE (quello nel gedcom)
			if( new File(nome).isFile() )
				return nome;
			for( String dir : Globale.preferenze.alberoAperto().cartelle ) {
				// Cartella media + percorso FILE
				String percorsoRicostruito = dir + '/' + nome;
				if( new File(percorsoRicostruito).isFile() )
					return percorsoRicostruito;
				// Cartella media + nome del FILE
				String percorsoFile = dir + '/' + new File(nome).getName();
				if( new File(percorsoFile).isFile() )
					return percorsoFile;
			}
			Object stringa = m.getExtension("cache");
			// A volte è String a volte JsonPrimitive, non ho capito bene perché
			if( stringa != null ) {
				String percorsoCache;
				if( stringa instanceof String )
					percorsoCache = (String) stringa;
				else
					percorsoCache = ((JsonPrimitive)stringa).getAsString();
				if( new File(percorsoCache).isFile() )
					return percorsoCache;
			}
		}
		return null;
	}

	static Bitmap generaIcona( ImageView vista, int icona, String testo ) {
		LayoutInflater inflater = (LayoutInflater) vista.getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		View inflated = inflater.inflate( icona, null );
		RelativeLayout frameLayout = inflated.findViewById( R.id.icona );
		((TextView)frameLayout.findViewById( R.id.icona_testo ) ).setText( testo );
		frameLayout.setDrawingCacheEnabled( true );
		frameLayout.measure( View.MeasureSpec.makeMeasureSpec( 0, View.MeasureSpec.UNSPECIFIED ),
				View.MeasureSpec.makeMeasureSpec( 0, View.MeasureSpec.UNSPECIFIED ) );
		frameLayout.layout( 0, 0, frameLayout.getMeasuredWidth(), frameLayout.getMeasuredHeight() );
		frameLayout.buildDrawingCache( true );
		return frameLayout.getDrawingCache();
	}

	// Salva in cache un'immagine trovabile in internet per poi riusarla
	static class ImboscaImmagine extends AsyncTask<URL,Void,String> {
		Media media;
		ImboscaImmagine( Media media ) {
			this.media = media;
		}
		protected String doInBackground( URL... url ) {
			try {
				File cartellaCache = new File( Globale.contesto.getCacheDir().getPath() + "/" + Globale.preferenze.idAprendo );
				if( !cartellaCache.exists() ) {
					// Elimina extension "cache" da tutti i Media
					ListaMedia visitaMedia = new ListaMedia( Globale.gc, true );
					Globale.gc.accept( visitaMedia );
					for( Map.Entry<Media,Object> dato : visitaMedia.listaMedia.entrySet() )
						if( dato.getKey().getExtension("cache") != null )
							dato.getKey().putExtension( "cache", null );
					cartellaCache.mkdir();
				}
				String estensione = FilenameUtils.getName( url[0].getPath() );
				if( estensione.lastIndexOf('.') > -1 )
					estensione = estensione.substring( estensione.lastIndexOf('.')+1 );
				String ext;
				switch( estensione ) {
					case "png":
						ext = "png";
						break;
					case "gif":
						ext = "gif";
						break;
					case "bmp":
						ext = "bmp";
						break;
					case "jpg":
					case "jpeg":
					default:
						ext = "jpg";
				}
				File cache = fileNomeProgressivo( cartellaCache.getPath(), "img." + ext );
				FileUtils.copyURLToFile( url[0], cache );
				return cache.getPath();
			} catch( Exception e ) {
				e.printStackTrace();
			}
			return null;
		}
		protected void onPostExecute( String percorso) {
			if( percorso != null )
				media.putExtension( "cache", percorso );
		}
	}

	// Scarica asincronicamente l'immagine da internet
	static class ZuppaMedia extends AsyncTask<String, Integer, Bitmap> {
		ImageView vistaImmagine;
		ProgressBar circo;
		Media media;
		URL url;
		ZuppaMedia( ImageView vistaImmagine, ProgressBar circo, Media media ) {
			this.vistaImmagine = vistaImmagine;
			this.circo = circo;
			this.media = media;
		}
		@Override
		protected Bitmap doInBackground(String... parametri) {
			Bitmap bitmap;
			try {
				// Prima prova con l'url diretto a un'immagine
				// Todo Eliminare? l'url diretto all'immagine è gestito da Picasso
				url = new URL( parametri[0] );
				InputStream inputStream = url.openConnection().getInputStream();
				BitmapFactory.Options opzioni = new BitmapFactory.Options();
				opzioni.inJustDecodeBounds = true;	// prende solo le info dell'immagine senza scaricarla
				BitmapFactory.decodeStream( inputStream, null, opzioni );
				// Se non lo trova cerca l'immagine principale in una pagina internet
				if( opzioni.outWidth == -1 ) {
					Connection connessione = Jsoup.connect(parametri[0]);
					//if (connessione.equals(bitmap)) {    // TODO: verifica che un address sia associato all'hostname
					Document doc = connessione.get();
					List<Element> lista = doc.select("img");
					if( lista.isEmpty() ) { // Pagina web trovata ma senza immagini
						vistaImmagine.setTag( R.id.tag_tipo_file, 3 );
						return generaIcona( vistaImmagine, R.layout.media_mondo, url.getProtocol() );	// ritorna una bitmap
					}
					int maxDimensioniConAlt = 1;
					int maxDimensioni = 1;
					int maxLunghezzaAlt = 0;
					int maxLunghezzaSrc = 0;
					Element imgGrandeConAlt = null;
					Element imgGrande = null;
					Element imgAltLungo = null;
					Element imgSrcLungo = null;
					for( Element img : lista ) {
						int larga, alta;
						if (img.attr("width").isEmpty()) larga = 1;
						else larga = Integer.parseInt(img.attr("width"));
						if (img.attr("height").isEmpty()) alta = 1;
						else alta = Integer.parseInt(img.attr("height"));
						if( larga * alta > maxDimensioniConAlt  &&  !img.attr("alt").isEmpty() ) {    // la più grande con alt
							imgGrandeConAlt = img;
							maxDimensioniConAlt = larga * alta;
						}
						if( larga * alta > maxDimensioni ) {    // la più grande anche senza alt
							imgGrande = img;
							maxDimensioni = larga * alta;
						}
						if( img.attr("alt").length() > maxLunghezzaAlt ) { // quella con l'alt più lungo
							imgAltLungo = img;
							maxLunghezzaAlt = img.attr( "alt" ).length();
						}
						if( img.attr("src").length() > maxLunghezzaSrc ) { // quella col src più lungo
							imgSrcLungo = img;
							maxLunghezzaSrc = img.attr("src").length();
						}
					}
					String percorso = null;
					if( imgGrandeConAlt != null )
						percorso = imgGrandeConAlt.absUrl( "src" );  //absolute URL on src
					else if( imgGrande != null )
						percorso = imgGrande.absUrl( "src" );
					else if( imgAltLungo != null )
						percorso = imgAltLungo.absUrl( "src" );
					else if( imgSrcLungo != null )
						percorso = imgSrcLungo.absUrl( "src" );
					url = new URL(percorso);
					inputStream = url.openConnection().getInputStream();
					BitmapFactory.decodeStream(inputStream, null, opzioni);
				}
				// Infine cerca di caricare l'immagine vera e propria ridimensionandola
				if( opzioni.outWidth > vistaImmagine.getWidth() )
					opzioni.inSampleSize = opzioni.outWidth / (vistaImmagine.getWidth()+1);
				inputStream = url.openConnection().getInputStream();
				opzioni.inJustDecodeBounds = false;	// Scarica l'immagine
				bitmap = BitmapFactory.decodeStream( inputStream, null, opzioni );
			} catch( Exception e ) {
				return null;
			}
			return bitmap;
		}
		@Override
		protected void onPostExecute( Bitmap bitmap ) {
			if( bitmap != null ) {
				vistaImmagine.setImageBitmap( bitmap );
				vistaImmagine.setTag( R.id.tag_tipo_file, 1 );
				vistaImmagine.setTag( R.id.tag_percorso, url.toString() );	// usato da Immagine
				new ImboscaImmagine(media).execute( url );
			} else
				vistaImmagine.setImageResource( R.drawable.manichino );
			if( circo != null ) // può arrivare molto in ritardo quando la pagina non esiste più
				circo.setVisibility( View.GONE );
		}
	}

	// Metodi per acquisizione immagini:

	// Propone una bella lista di app per acquisire immagini
	public static void appAcquisizioneImmagine( final Context contesto, final Fragment frammento, final int codice, final MediaContainer contenitore ) {
		// Richiesta permesso accesso file in memoria
		int perm = ContextCompat.checkSelfPermission( contesto, Manifest.permission.WRITE_EXTERNAL_STORAGE );
		if( perm == PackageManager.PERMISSION_DENIED ) {
			ActivityCompat.requestPermissions( (AppCompatActivity) contesto, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, codice );
			return;
		}
		// Colleziona gli intenti utili per acquisire immagini
		List<ResolveInfo> listaRisolvi = new ArrayList<>();
		final List<Intent> listaIntenti = new ArrayList<>();
		// Camere
		Intent intentoCamera = new Intent("android.media.action.IMAGE_CAPTURE");
		for( ResolveInfo info : contesto.getPackageManager().queryIntentActivities(intentoCamera,0) ) {
			Intent finalIntent = new Intent( intentoCamera );
			finalIntent.setComponent( new ComponentName(info.activityInfo.packageName, info.activityInfo.name) );
			listaIntenti.add(finalIntent);
			listaRisolvi.add( info );
		}
		// Gallerie
		Intent intentoGalleria = new Intent( Intent.ACTION_GET_CONTENT );
		intentoGalleria.setType("image/*");
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ) { // da KitKat: Android 4.4, api level 19
			String[] mimeTypes = { "image/*", "audio/*", "video/*", "application/*", "text/*" };
			intentoGalleria.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
		}
		for( ResolveInfo info : contesto.getPackageManager().queryIntentActivities(intentoGalleria,0) ) {
			Intent finalIntent = new Intent( intentoGalleria );
			finalIntent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
			s.l( info.activityInfo.packageName +"   "+ info.activityInfo.name);
			listaIntenti.add( finalIntent );
			listaRisolvi.add( info );
		}
		// Media vuoto
		if( Globale.preferenze.esperto && codice != 5173 ) { // tranne che per la scelta di file in Immagine
			Intent intento = new Intent( contesto, Immagine.class );
			ResolveInfo info = contesto.getPackageManager().resolveActivity( intento, 0 );
			intento.setComponent(new ComponentName(info.activityInfo.packageName,info.activityInfo.name));
			listaIntenti.add( intento );
			listaRisolvi.add( info );
		}
		AlertDialog.Builder dialog = new AlertDialog.Builder( contesto );
		dialog.setAdapter( faiAdattatore( contesto, listaRisolvi ),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						Intent intento = listaIntenti.get(id);
						if( intento.getComponent().getPackageName().equals("app.familygem") ) {
							Media med;
							if( codice==4173 || codice==2173 ) { // Media inline
								med = new Media();
								med.setFileTag( "FILE" );
								contenitore.addMedia( med );
								Ponte.manda( contenitore, "contenitore" );
							} else // Media condiviso
								med = Galleria.nuovoMedia( contenitore );
							med.setFile( "" );
							Ponte.manda( med, "oggetto" );
							contesto.startActivity( intento );
						} else if( frammento != null )
							frammento.startActivityForResult( intento, codice ); // Così il risultato ritorna al frammento
						else
							((AppCompatActivity)contesto).startActivityForResult( intento, codice );
					}
				}).show();
	}
	// Strettamente legato a quello qui sopra
	private static ArrayAdapter<ResolveInfo> faiAdattatore( final Context contesto, final List<ResolveInfo> listaRisolvi) {
		return new ArrayAdapter<ResolveInfo>( contesto, R.layout.pezzo_app, R.id.intento_titolo, listaRisolvi ) {
			@Override
			public View getView( int posizione, View vista, ViewGroup genitore ) {
				View view = super.getView( posizione, vista, genitore );
				ResolveInfo info = listaRisolvi.get( posizione );
				ImageView image = view.findViewById(R.id.intento_icona);
				TextView textview = view.findViewById(R.id.intento_titolo);
				if( info.activityInfo.packageName.equals("app.familygem") ) {
					image.setImageResource( R.drawable.manichino );
					textview.setText( R.string.empty_media );
				} else {
					image.setImageDrawable( info.loadIcon(contesto.getPackageManager()) );
					textview.setText( info.loadLabel(contesto.getPackageManager()).toString() );
				}
				return view;
			}
		};
	}

	// Salva il file acquisito e propone di ritagliarlo se è un'immagine
	// ritorna true se apre il dialogo e quindi bisogna bloccare l'aggiornamento dell'attività
	// Todo: rinomina tipo stoccaImmagine
	static boolean ritagliaImmagine( final Context contesto, final Fragment frammento, Intent data, Media media ) {
		final File fileMedia = settaMedia( contesto, data, media );
		if( fileMedia == null )	return false;
		String tipoMime = URLConnection.guessContentTypeFromName( fileMedia.getName() );
		if( tipoMime != null && tipoMime.startsWith("image/") ) {
			ImageView vistaImmagine = new ImageView( contesto );
			U.dipingiMedia( media, vistaImmagine, null );
			Globale.mediaCroppato = media; // Media in attesa di essere aggiornato col nuovo percorso file
			AlertDialog.Builder costruttore = new AlertDialog.Builder( contesto );
			costruttore.setMessage( R.string.want_crop_image )
					.setView(vistaImmagine)
					.setPositiveButton( android.R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialog, int id ) {
							tagliaImmagine( contesto, fileMedia, frammento );
						}
					}).setNegativeButton( android.R.string.no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick( DialogInterface dialog, int which ) {
					((AppCompatActivity)contesto).recreate();
					Globale.editato = true;
				}
			} )	.create().show();
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams( FrameLayout.LayoutParams.MATCH_PARENT, 400 );
			vistaImmagine.setLayoutParams( params ); // l'assegnazione delle dimensioni deve venire DOPO la creazione del dialogo
			return true;
		}
		return false;
	}

	static void tagliaImmagine( Context contesto, File fileMedia, Fragment frammento ) {
		File dirMemoria = new File( contesto.getExternalFilesDir(null) +"/"+ Globale.preferenze.idAprendo );
		if( !dirMemoria.exists() )
			dirMemoria.mkdir();
		File fileDestinazione;
		if( fileMedia.getAbsolutePath().startsWith(dirMemoria.getAbsolutePath()) )
			fileDestinazione = fileMedia; // File già nella cartella memoria vengono sovrascritti
		else
			fileDestinazione = fileNomeProgressivo( dirMemoria.getAbsolutePath(), fileMedia.getName() );
		Intent intento = CropImage.activity( Uri.fromFile(fileMedia) )
				.setOutputUri( Uri.fromFile(fileDestinazione) ) // cartella in memoria esterna
				.setGuidelines( CropImageView.Guidelines.OFF )
				.setBorderLineThickness( 1 )
				.setBorderCornerThickness( 6 )
				.setBorderCornerOffset( -3 )
				.setCropMenuCropButtonTitle( contesto.getText(R.string.done) )
				.getIntent( contesto );
		if( frammento != null )
			frammento.startActivityForResult( intento, CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE );
		else
			((AppCompatActivity)contesto).startActivityForResult( intento, CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE );
	}

	// Imposta in un Media il percorso fornito dalle app dispensatrici. Restituisce il File trovato oppure null
	static File settaMedia( Context contesto, Intent data, Media media ) {
		File fileMedia = null;
		try {
			Uri uri = data.getData();
			String percorso = null;
			if( uri != null ) percorso = U.uriPercorsoFile( uri );
			if( percorso != null && percorso.lastIndexOf('/') > 0 ) {    // se è un percorso completo del file
				// Punta direttamente il file
				fileMedia = new File( percorso );
			} else {
				// Salva il file nella memoria esterna della app  /mnt/shell/emulated/0/Android/data/lab.gedcomy/files/
				File dirMemoria = new File( contesto.getExternalFilesDir(null) +"/"+ Globale.preferenze.idAprendo );
				if( !dirMemoria.exists() )
					dirMemoria.mkdir();
				// File di qualsiasi tipo
				if( percorso != null ) {    // è solo il nome del file 'pippo.png'
					InputStream input = contesto.getContentResolver().openInputStream( uri );
					fileMedia = U.fileNomeProgressivo( dirMemoria.getAbsolutePath(), percorso );
					FileUtils.copyInputStreamToFile( input, fileMedia );
				// In alcuni casi (telefoni vecchi?) immagini da camera passano solo come bitmap A BASSA RISOLUZIONE negli extra
				} else if( data.getExtras() != null ) {
					Bitmap bitmap = (Bitmap) data.getExtras().get("data");
					// TODo usa la bitmap solo come anteprima: "La foto è stata scattata, ma non è stato possibile importarla."
					fileMedia = U.fileNomeProgressivo( dirMemoria.getAbsolutePath(), "img.jpg" );
					OutputStream os = new BufferedOutputStream(new FileOutputStream(fileMedia));
					bitmap.compress( Bitmap.CompressFormat.JPEG, 99, os );
					os.close();
				}
			}
			if( fileMedia != null ) {
				// Aggiunge il percorso della cartella nel Cassetto in preferenze
				if( Globale.preferenze.alberoAperto().cartelle.add( fileMedia.getParent() ) ) // true se ha aggiunto la cartella
					Globale.preferenze.salva();
				media.setFile( fileMedia.getAbsolutePath() );
			}
		} catch( Exception e ) {
			String msg = e.getLocalizedMessage()!=null ? e.getLocalizedMessage() : contesto.getString(R.string.something_wrong);
			Toast.makeText( contesto, msg, Toast.LENGTH_LONG ).show();
		}
		return fileMedia;
	}

	// Se in quella cartella esiste già un file con quel nome lo incrementa con 1 2 3...
	static File fileNomeProgressivo( String dir, String nome ) {
		File file = new File( dir, nome );
		int incremento = 0;
		while( file.exists() ) {
			incremento++;
			file = new File( dir, nome.substring(0,nome.lastIndexOf('.'))
					+ incremento + nome.substring(nome.lastIndexOf('.'),nome.length()));
		}
		return file;
	}

	// Conclude la procedura di ritaglio di un'immagine
	static void fineRitaglioImmagine( Intent data ) {
		CropImage.ActivityResult risultato = CropImage.getActivityResult(data);
		Uri uri = risultato.getUri();
		Globale.mediaCroppato.setFile( U.uriPercorsoFile( uri ) );
	}

	// Risposta a tutte le richieste di permessi per Android 6+
	static void risultatoPermessi( Context contesto, int codice, String[] permessi, int[] accordi, Object oggetto ) {
		if( accordi.length > 0 && accordi[0] == PackageManager.PERMISSION_GRANTED ) {
			if( codice == 4546 ) { // Da Fragment Galleria
				Fragment galleria = ((AppCompatActivity)contesto).getSupportFragmentManager().findFragmentById( R.id.contenitore_fragment );
				appAcquisizioneImmagine( contesto, galleria, codice, null );
			} else
				appAcquisizioneImmagine( contesto, null, codice, (MediaContainer)oggetto );
		} else {
			String permesso = permessi[0].substring( permessi[0].lastIndexOf('.')+1 );
			Toast.makeText( contesto, contesto.getString(R.string.not_granted,permesso), Toast.LENGTH_SHORT ).show();
		}
	}

	// Metodi di creazione di elementi di lista

	// aggiunge a un Layout una generica voce titolo-testo
	// TODO: DEPRECARLO prima o poi
	public static void metti( LinearLayout scatola, String tit, String cosa ) {
		View vistaPezzo = LayoutInflater.from(scatola.getContext()).inflate( R.layout.pezzo_fatto, scatola, false );
		scatola.addView( vistaPezzo );
		((TextView)vistaPezzo.findViewById( R.id.fatto_titolo )).setText( tit );
		TextView vistaTesto = vistaPezzo.findViewById( R.id.fatto_testo );
		if( cosa == null ) vistaTesto.setVisibility( View.GONE );
		else {
			vistaTesto.setText( cosa );
			((TextView)vistaPezzo.findViewById( R.id.fatto_edita )).setText( cosa );
		}
		((Activity)scatola.getContext()).registerForContextMenu( vistaPezzo );
	}

	// Compone il testo coi dettagli di un individuo
	static void dettagli( Person tizio, TextView vistaDettagli ) {
		String anni = U.dueAnni( tizio, true );
		String luoghi = Anagrafe.dueLuoghi( tizio );
		if( anni.isEmpty() && luoghi.isEmpty() ) {
			vistaDettagli.setVisibility( View.GONE );
		} else {
			if( ( anni.length() > 10 || luoghi.length() > 20 ) && ( !anni.isEmpty() && !luoghi.isEmpty() ) )
				anni += "\n" + luoghi;
			else
				anni += "   " + luoghi;
			vistaDettagli.setText( anni.trim() );
		}
	}

	public static View mettiIndividuo( LinearLayout scatola, Person persona, String ruolo ) {
		View vistaIndi = LayoutInflater.from(scatola.getContext()).inflate( R.layout.pezzo_individuo, scatola, false);
		scatola.addView( vistaIndi );
		TextView vistaRuolo = vistaIndi.findViewById( R.id.indi_ruolo );
		if( ruolo == null ) vistaRuolo.setVisibility( View.GONE );
		else vistaRuolo.setText( ruolo );
		TextView vistaNome = vistaIndi.findViewById( R.id.indi_nome );
		String nome = epiteto(persona);
		if( nome.isEmpty() && ruolo != null ) vistaNome.setVisibility( View.GONE );
		else vistaNome.setText( nome );
		TextView vistaTitolo = vistaIndi.findViewById(R.id.indi_titolo);
		String titolo = U.titolo( persona );
		if( titolo.isEmpty() ) vistaTitolo.setVisibility( View.GONE );
		else vistaTitolo.setText( titolo );
		dettagli( persona, (TextView) vistaIndi.findViewById( R.id.indi_dettagli ) );
		unaFoto( persona, (ImageView)vistaIndi.findViewById(R.id.indi_foto) );
		if( !U.morto(persona) )
			vistaIndi.findViewById( R.id.indi_lutto ).setVisibility( View.GONE );
		if( U.sesso(persona) == 1 )
			vistaIndi.findViewById(R.id.indi_carta).setBackgroundResource( R.drawable.casella_maschio );
		if( U.sesso(persona) == 2 )
			vistaIndi.findViewById(R.id.indi_carta).setBackgroundResource( R.drawable.casella_femmina );
		vistaIndi.setTag( persona.getId() );
		return vistaIndi;
	}

	// Tutte le note di un oggetto
	public static void mettiNote( final LinearLayout scatola, final Object contenitore, boolean dettagli ) {
		for( final Note nota : ((NoteContainer)contenitore).getAllNotes( Globale.gc ) ) {
			View vistaNota = LayoutInflater.from(scatola.getContext()).inflate( R.layout.pezzo_nota, scatola, false);
			scatola.addView( vistaNota );
			((TextView)vistaNota.findViewById( R.id.nota_testo )).setText( nota.getValue() );
			int quanteCitaFonti = nota.getSourceCitations().size();
			TextView vistaCitaFonti = vistaNota.findViewById( R.id.nota_fonti );
			if( quanteCitaFonti > 0 && dettagli ) vistaCitaFonti.setText( String.valueOf(quanteCitaFonti) );
			else vistaCitaFonti.setVisibility( View.GONE );
			if( dettagli ) {
				vistaNota.setTag( R.id.tag_oggetto, nota );
				vistaNota.setTag( R.id.tag_contenitore, contenitore );	// inutile. da tenere per un'eventuale Quaderno delle note
				if( scatola.getContext() instanceof Individuo ) { // Fragment individuoEventi
					((AppCompatActivity)scatola.getContext()).getSupportFragmentManager()
							.findFragmentByTag( "android:switcher:" + R.id.schede_persona + ":1" )	// non garantito in futuro
							.registerForContextMenu( vistaNota );
				} else	// ok nelle AppCompatActivity
					((AppCompatActivity)scatola.getContext()).registerForContextMenu( vistaNota );
				vistaNota.setOnClickListener( new View.OnClickListener() {
					public void onClick( View v ) {
						Ponte.manda( nota, "oggetto" );
						Ponte.manda( contenitore, "contenitore" );
						scatola.getContext().startActivity( new Intent( scatola.getContext(), Nota.class ) );
					}
				} );
			}
		}
	}

	public static void scollegaNota( Note nota, Object contenitore, View vista ) {
		//s.l("Scollego " + nota + " da " + contenitore );
		List<NoteRef> lista = ((NoteContainer)contenitore).getNoteRefs();
		for( NoteRef ref : lista )
			if( ref.getNote(Globale.gc).equals( nota ) ) {
				lista.remove( ref );
				break;
			}
		((NoteContainer)contenitore).setNoteRefs( lista );
		vista.setVisibility( View.GONE );
	}

	// Elimina una Nota inlinea o condivisa
	public static void eliminaNota( Note nota, Object contenitore, View vista ) {
		if( nota.getId() != null ) {	// nota OBJECT
			// Prima rimuove i ref alla nota con un bel Visitor
			RiferimentiNota eliminatoreNote = new RiferimentiNota( nota.getId(), true );
			Globale.gc.accept( eliminatoreNote );
			Globale.gc.getNotes().remove( nota );	// ok la rimuove se è un'object note
			//Globale.gc.createIndexes();	// pare proprio non necessario
			if( Globale.gc.getNotes().isEmpty() )
				Globale.gc.setNotes( null );
		} else { // nota LOCALE
			NoteContainer nc = (NoteContainer) contenitore;
			nc.getNotes().remove( nota ); // rimuove solo se è una nota locale, non se object note
			if( nc.getNotes().isEmpty() )
				nc.setNotes( null );
		}
		if( vista != null )
			vista.setVisibility( View.GONE );
	}

	// Elenca tutti i media di un oggetto contenitore
	public static void mettiMedia( LinearLayout scatola, Object contenitore, boolean dettagli ) {
		RecyclerView griglia = new RecyclerView( scatola.getContext() );
		griglia.setHasFixedSize( true );
		RecyclerView.LayoutManager gestoreLayout = new GridLayoutManager( scatola.getContext(), dettagli?2:3 );
		griglia.setLayoutManager( gestoreLayout );
		Map<Media,Object> listaMedia = new LinkedHashMap<>();
		for( Media med : ((MediaContainer)contenitore).getAllMedia(Globale.gc) )
			listaMedia.put( med, contenitore );
		AdattatoreGalleriaMedia adattatore = new AdattatoreGalleriaMedia( listaMedia, dettagli );
		griglia.setAdapter( adattatore );
		scatola.addView( griglia );
	}

	// Di un oggetto inserisce le citazioni alle fonti
	public static void citaFonti( final LinearLayout scatola, final Object contenitore  ) {
		List<SourceCitation> listaCitaFonti;
		if( contenitore instanceof Note )	// Note non estende SourceCitationContainer
			listaCitaFonti = ( (Note) contenitore ).getSourceCitations();
		else listaCitaFonti = ((SourceCitationContainer)contenitore).getSourceCitations();
		for( final SourceCitation citaz : listaCitaFonti ) {
			View vistaCita = LayoutInflater.from( scatola.getContext() ).inflate( R.layout.pezzo_citazione_fonte, scatola, false );
			scatola.addView( vistaCita );
			if( citaz.getSource(Globale.gc) != null )    // source CITATION
				( (TextView) vistaCita.findViewById( R.id.fonte_titolo ) ).setText( Biblioteca.titoloFonte( citaz.getSource( Globale.gc ) ) );
			else // source NOTE, oppure Citazione di fonte che è stata eliminata
				vistaCita.findViewById( R.id.citazione_fonte ).setVisibility( View.GONE );
			String t = "";
			if( citaz.getValue() != null ) t += citaz.getValue() + "\n";
			if( citaz.getPage() != null ) t += citaz.getPage() + "\n";
			if( citaz.getDate() != null ) t += citaz.getDate() + "\n";
			if( citaz.getText() != null ) t += citaz.getText() + "\n";    // vale sia per sourceNote che per sourceCitation
			TextView vistaTesto = vistaCita.findViewById( R.id.citazione_testo );
			if( t.isEmpty() ) vistaTesto.setVisibility( View.GONE );
			else vistaTesto.setText( t.substring( 0, t.length() - 1 ) );
			// Tutto il resto
			LinearLayout scatolaAltro = vistaCita.findViewById( R.id.citazione_note );
			mettiNote( scatolaAltro, citaz, false );
			mettiMedia( scatolaAltro, citaz, false );
			vistaCita.setTag( R.id.tag_oggetto, citaz );
			if( scatola.getContext() instanceof Individuo ) { // Fragment individuoEventi
				( (AppCompatActivity) scatola.getContext() ).getSupportFragmentManager()
						.findFragmentByTag( "android:switcher:" + R.id.schede_persona + ":1" )
						.registerForContextMenu( vistaCita );
			} else	// AppCompatActivity
				((AppCompatActivity)scatola.getContext()).registerForContextMenu( vistaCita );

			vistaCita.setOnClickListener( new View.OnClickListener() {
				public void onClick( View v ) {
					Ponte.manda( citaz, "oggetto" );
					Ponte.manda( contenitore, "contenitore" );
					scatola.getContext().startActivity( new Intent( scatola.getContext(), CitazioneFonte.class ) );
				}
			} );
		}
	}

	// usato da dettaglio.CitazioneFonte e dettaglio.Immagine
	public static void linkaFonte( final LinearLayout scatola, final Source fonte ) {
		View vistaFonte = LayoutInflater.from(scatola.getContext()).inflate( R.layout.pezzo_fonte, scatola, false );
		scatola.addView( vistaFonte );
		((TextView)vistaFonte.findViewById( R.id.fonte_titolo )).setText( Biblioteca.titoloFonte(fonte) );
		vistaFonte.setTag( R.id.tag_oggetto, fonte );
		((AppCompatActivity)scatola.getContext()).registerForContextMenu( vistaFonte );
		vistaFonte.setOnClickListener( new View.OnClickListener() {
			public void onClick( View v ) {
				Ponte.manda( fonte, "oggetto" );
				scatola.getContext().startActivity( new Intent( scatola.getContext(), Fonte.class) );
			}
		} );
	}

	public static void linkaPersona( final LinearLayout scatola, final Person p, final int scheda ) {
		View vistaPersona = LayoutInflater.from(scatola.getContext()).inflate( R.layout.pezzo_individuo_piccolo, scatola, false );
		scatola.addView( vistaPersona );
		U.unaFoto( p, (ImageView)vistaPersona.findViewById(R.id.collega_foto) );
		((TextView)vistaPersona.findViewById( R.id.collega_nome )).setText( U.epiteto(p) );
		String dati = U.dueAnni(p,false);
		TextView vistaDettagli = vistaPersona.findViewById( R.id.collega_dati );
		if( dati.isEmpty() ) vistaDettagli.setVisibility( View.GONE );
		else vistaDettagli.setText( dati );
		if( !U.morto( p ) )
			vistaPersona.findViewById( R.id.collega_lutto ).setVisibility( View.GONE );
		if( U.sesso(p) == 1 )
			vistaPersona.findViewById(R.id.collega_carta).setBackgroundResource( R.drawable.casella_maschio );
		if( U.sesso(p) == 2 )
			vistaPersona.findViewById(R.id.collega_carta).setBackgroundResource( R.drawable.casella_femmina );
		vistaPersona.setOnClickListener( new View.OnClickListener() {
			public void onClick( View v ) {
				Intent intento = new Intent( scatola.getContext(), Individuo.class );
				intento.putExtra( "idIndividuo", p.getId() );
				intento.putExtra( "scheda", scheda );
				scatola.getContext().startActivity( intento );
			}
		} );
	}

	public static void cambiamenti( final LinearLayout scatola, Change cambi ) {
		if( cambi != null ) {
			View vistaPezzo = LayoutInflater.from( scatola.getContext() ).inflate( R.layout.pezzo_fatto, scatola, false );
			scatola.addView( vistaPezzo );
			( (TextView) vistaPezzo.findViewById( R.id.fatto_titolo ) ).setText( R.string.change_date );
			TextView vistaTesto = vistaPezzo.findViewById( R.id.fatto_testo );
			String dataOra = cambi.getDateTime().getValue() + " - " + cambi.getDateTime().getTime();
			if( dataOra.isEmpty() ) vistaTesto.setVisibility( View.GONE );
			else vistaTesto.setText( dataOra );
			LinearLayout scatolaNote = vistaPezzo.findViewById( R.id.fatto_note );
			for( Estensione altroTag : trovaEstensioni( cambi ) )
				metti( scatolaNote, altroTag.nome, altroTag.testo );
			// Grazie al mio contributo la data cambiamento può avere delle note
			U.mettiNote( scatolaNote, cambi, true );
		}
	}

	// Chiede conferma di eliminare un elemento
	public static boolean preserva( Object cosa ) {
		// todo Conferma elimina
		return false;
	}

	public static void salvaJson() {
		if( Globale.preferenze.autoSalva )
			salvaJson( Globale.gc, Globale.preferenze.idAprendo );
	}

	static void salvaJson( Gedcom gc, int idAlbero ) {
		try {
			FileUtils.writeStringToFile(
					new File( Globale.contesto.getFilesDir(), idAlbero + ".json" ),
					new JsonParser().toJson( gc )
			);
		} catch (IOException e) {
			Toast.makeText( Globale.contesto, e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
		}
	}

	static int castaJsonInt( Object ignoto ) {
		if( ignoto instanceof Integer ) return (int) ignoto;
		else return ((JsonPrimitive)ignoto).getAsInt();
	}

	// Valuta se ci sono individui collegabili a un individuo
	static boolean ciSonoIndividuiCollegabili( Person piolo ) {
		int numTotali = Globale.gc.getPeople().size();
		int numFamili = Anagrafe.quantiFamiliari( piolo );
		return numTotali > numFamili+1;
	}
}