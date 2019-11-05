/*******************************************************************************
 * Copyright (C) 2019 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.gouv.education.acrennes.alambic.api;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;

public class WebServiceApi {
	private final static String CONTENT_TYPE_PATTERN = "^([^;]+)(;\\W*charset=(.+))?";
	private final static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	private final static String DEFAULT_CONTENT_TYPE = MediaType.APPLICATION_JSON;
	
	private HttpUriRequest request = null;
	private List<Integer> successResponseCodes = new ArrayList<>();

	public WebServiceApi(final CallableContext context, Element node) throws AlambicException {
		// Get the API URI
		String uri = context.resolveString(node.getAttributeValue("uri"));
		
		// Get the API success codes list
		List<Integer> success_rsp_codes = new ArrayList<>();
		XPathFactory xpf;
		xpf = XPathFactory.instance();
		XPathExpression<Element> xpath = xpf.compile("./response_codes/code[@type=\"success\"]", Filters.element());
		List<Element> successCodes = xpath.evaluate(node);
		if (null != successCodes && !successCodes.isEmpty()) {
			for (Element code : successCodes) {
				success_rsp_codes.add(Integer.parseInt(code.getText()));
			}
		} else {
			success_rsp_codes.add(Integer.valueOf(HttpStatus.SC_OK));
		}

		// Set the request headers
		Map<String, String> headers_map = new HashMap<>();
		Element headersElts = node.getChild("headers");
		if (null != headersElts && 0 < headersElts.getChildren().size()) {
			for (Element headerElt : headersElts.getChildren()) {
				headers_map.put(headerElt.getAttributeValue("name"), headerElt.getText());
			}
		}
		
		// Get the API payload (may be no-one)
		Element payLoadElt = node.getChild("payload");
		String pay_load = (null != payLoadElt) ? getPayLoad(headers_map, payLoadElt) : null;
		
		// Instantiate the request according to the method
		String method = node.getAttributeValue("method");
		if (null == method || StringUtils.isBlank(method)) {
			throw new AlambicException("Le protocole HTTP doit être précisé (GET, POST, PUT...)");
		}

		this.request = buildRequest(uri, method, headers_map, pay_load, success_rsp_codes);
	}
	
	private String getPayLoad(final Map<String, String> headers_map, final Element payLoadElt) {
		String payLoad = StringEscapeUtils.unescapeXml(payLoadElt.getText());
		if (StringUtils.isNotBlank(headers_map.get(HttpHeaders.CONTENT_TYPE)) && headers_map.get(HttpHeaders.CONTENT_TYPE).matches(".*\\W?[xX][mM][lL]\\W.*")) {
			XMLOutputter outputter = new XMLOutputter();
			payLoad = outputter.outputString(payLoadElt);
			payLoad = payLoad.replaceAll("</?payload>", "");
		}
		return payLoad;
	}
	
	public WebServiceApi(final String uri, final String method, final Map<String, String> headers, final String payload, final List<Integer> successResponseCodes) throws AlambicException {
		this.request = buildRequest(uri, method, headers, payload, successResponseCodes);
	}
	
	public HttpUriRequest buildRequest(final String uri, final String method, final Map<String, String> headers, final String payload, final List<Integer> successResponseCodes) throws AlambicException {
		HttpUriRequest request = null;
		
		this.successResponseCodes = successResponseCodes;
		
		// Instantiate the request according to the method
		switch (method) {
		case HttpMethod.GET:
			request = new HttpGet(uri);
			break;
		case HttpMethod.PUT:
			request = new HttpPut(uri);
			break;
		case HttpMethod.POST:
			request = new HttpPost(uri);
			break;
		case HttpMethod.DELETE:
			request = new HttpDelete(uri);
			break;
		default:
			throw new AlambicException("Méthode HTTP non supportée '" + method + "'");
		}

		// Set the request headers
		if (null != headers && 0 < headers.size()) {
			for (String header : headers.keySet()) {
				request.addHeader(header, headers.get(header));
			}
		}
		
		// Set payload
		if (StringUtils.isNotBlank(payload)) {
			((HttpEntityEnclosingRequest) request).setEntity(new StringEntity(payload, getContentType(headers)));
		}
		
		return request;
	}

	public HttpUriRequest getRequest() {
		return this.request;
	}

	public String getSuccessResponseCodes() {
		return String.join(",",
				(List<String>) this.successResponseCodes.stream().map(new Function<Integer, String>() {
					@Override
					public String apply(Integer t) {
						return String.valueOf(t);
					}
				}).collect(Collectors.toList()));
	}

	public boolean isSuccessful(HttpResponse response) {
		return this.successResponseCodes.contains(response.getStatusLine().getStatusCode());
	}

	@Override
	public String toString() {
		return String.format(
				"{\"protocole\":\"%s\",\"method\":\"%s\",\"uri\":\"%s\",\"headers\":[%s]}",
				this.request.getProtocolVersion(), this.request.getMethod(), this.request.getURI(),
				formatHeaders(this.request.getAllHeaders()));
	}
	
	// Get the request content type based on its headrers
	private ContentType getContentType(Map<String, String> headers) throws AlambicException {
		ContentType ct = ContentType.create(DEFAULT_CONTENT_TYPE, DEFAULT_CHARSET);
		
		if (null != headers && headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
			String contentType = headers.get(HttpHeaders.CONTENT_TYPE);
			Pattern contentTypeMatcher = Pattern.compile(CONTENT_TYPE_PATTERN);
			Matcher matcher = contentTypeMatcher.matcher(contentType);
			if (matcher.matches()) {
				ct = ContentType.create(matcher.group(1), (StringUtils.isNotBlank(matcher.group(3))) ? matcher.group(3) : DEFAULT_CHARSET.toString());
			} else {
				throw new AlambicException("The content type (specified from headers) '" + contentType + "' doesn't match the pattern '<mime type>; charset=<charset>' (example: 'application/json; charset=UTF-8')");
			}
		}
		
		return ct;
	}

	private String formatHeaders(Header[] headers) {
		List<String> list = new ArrayList<>();
		for (Header header : headers) {
			list.add(String.format("\"%s\":\"%s\"", header.getName(), header.getValue()));
		}
		return String.format("{%s}", String.join(",", list));
	}

}
