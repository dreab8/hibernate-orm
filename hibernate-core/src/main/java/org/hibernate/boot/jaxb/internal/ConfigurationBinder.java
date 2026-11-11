package org.hibernate.boot.jaxb.internal;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.hibernate.Internal;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.jaxb.internal.stax.ConfigurationEventReader;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.xsd.ConfigXsdSupport;
import org.hibernate.internal.util.config.ConfigurationException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

/**
 * @author Steve Ebersole
 */
public class ConfigurationBinder extends AbstractBinder<JaxbPersistenceImpl> {
	private final XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();
	private final boolean dtdAndExternalEntitiesAllowed;
	private JAXBContext jaxbContext;

	public ConfigurationBinder(ResourceStreamLocator resourceStreamLocator) {
		this( resourceStreamLocator, false );
	}

	/**
	 * @param dtdAndExternalEntitiesAllowed whether the document may declare a DTD and reference
	 * external entities; see {@link org.hibernate.cfg.MappingSettings#XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES}
	 */
	public ConfigurationBinder(ResourceStreamLocator resourceStreamLocator, boolean dtdAndExternalEntitiesAllowed) {
		super( resourceStreamLocator );
		this.dtdAndExternalEntitiesAllowed = dtdAndExternalEntitiesAllowed;
	}

	@Override
	public boolean isValidationEnabled() {
		return false;
	}

	// persistence.xml is parsed before Hibernate settings are resolved, so the caller
	// decides (e.g. from hibernate.properties or integration settings); secure by default.
	@Override
	public boolean isDtdAndExternalEntitiesAllowed() {
		return dtdAndExternalEntitiesAllowed;
	}

	@Override
	protected <X extends JaxbPersistenceImpl> Binding<X> doBind(
			XMLEventReader staxEventReader,
			StartElement rootElementStartEvent,
			Origin origin) {
		final XMLEventReader reader = new ConfigurationEventReader( staxEventReader, xmlEventFactory );
		final JaxbPersistenceImpl bindingRoot = jaxb(
				reader,
				ConfigXsdSupport.latestDescriptor().getSchema(),
				jaxbContext(),
				origin
		);
		//noinspection unchecked
		return new Binding<>( (X) bindingRoot, origin );
	}

	@Internal
	public JAXBContext jaxbContext() {
		if ( jaxbContext == null ) {
			try {
				jaxbContext = JAXBContext.newInstance( JaxbPersistenceImpl.class );
			}
			catch (JAXBException e) {
				throw new ConfigurationException( "Unable to build configuration.xml JAXBContext", e );
			}
		}
		return jaxbContext;
	}
}
