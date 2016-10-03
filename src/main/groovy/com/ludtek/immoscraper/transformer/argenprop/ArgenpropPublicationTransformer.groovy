package com.ludtek.immoscraper.transformer.argenprop

import java.util.regex.Matcher;

import org.ccil.cowan.tagsoup.Parser

import com.ludtek.immoscraper.model.Publication
import com.ludtek.immoscraper.transformer.PublicationTransformer

import groovy.util.slurpersupport.GPathResult

class ArgenpropPublicationTransformer implements PublicationTransformer {

	def BASE_URL = "http://www.argenprop.com"
	
	def PRICE_REGEX = ~/(.+) ([0-9\\.]+)/
	def ID_REGEX = ~/Propiedades\/Detalles\/([0-9]+)--.+/

	@Override
	public Publication parse(GPathResult rootnode) {
		Publication parsed = new Publication()
		
		def additionalInfoNode = rootnode.body.'**'.find { node ->
			node.name() == 'div' && node.@class == 'section additionalInfo'
		}
		
		def meta = rootnode.head.meta.collectEntries {
			[it.@name.text()?:it.@property.text(), it.@content.text()]
		}
		
		parsed.with {
			
			switch(meta["cXenseParse:precio"]) {
				case PRICE_REGEX:
					currency = m[0][1] == '$'?"ARS":"USD"
					amount = m[0][2].replaceAll("\\.","") as int
			}
			
			operation = meta["cXenseParse:tipooperacion"]
			propertyType = meta["cXenseParse:tipopropiedad"]
			
			partido = sanitize(meta["cXenseParse:partido"])
			localidad = sanitize(meta["cXenseParse:localidad"])
			provincia = sanitize(meta["cXenseParse:provincia"])
			barrio = sanitize(meta["cXenseParse:barrio"])
			
			url =  BASE_URL + meta["og:url"]
			
			title = sanitize(meta["og:title"].trim())
			
			description = sanitize(additionalInfoNode?.div?.find { it.@class == "fields" }?.text()?.trim())
			
			id = ID_REGEX.matcher(meta["og:url"])[0][1].toLong()
			
		}
		
		parsed
	}
	
	public static final String sanitize(String value) {
		value?value.replaceAll("<br>|\n|\t|\r|,|/", " "):value
	}
		
	@Override
	public Publication parse(String html) {
		def parser=new XmlSlurper(new Parser())
		parse(parser.parseText(html.trim()))
	}
	
	def getM() {
		Matcher.lastMatcher
	}

}
