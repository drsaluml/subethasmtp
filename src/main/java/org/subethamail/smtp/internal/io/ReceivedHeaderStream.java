/*
 *
 */
package org.subethamail.smtp.internal.io;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import org.subethamail.smtp.internal.util.TextUtils;

import com.github.davidmoten.guavamini.Preconditions;

/**
 * Prepends a Received: header at the beginning of the input stream.
 */
public final class ReceivedHeaderStream extends FilterInputStream {
    ByteArrayInputStream header;

    /**
     * Constructor.
     * 
     * @param softwareName
     *            A software name and version, or null if this information
     *            should not be printed
     * @param singleRecipient
     *            The single recipient of the message. If there are more than
     *            one recipients then this must be null.
     */
    /**
     * @param in
     * @param heloHost
     * @param host
     * @param whoami
     * @param softwareName
     * @param id
     * @param singleRecipient
     */
    public ReceivedHeaderStream(InputStream in, Optional<String> heloHost, InetAddress host,
            String whoami, Optional<String> softwareName, String id,
            Optional<String> singleRecipient) {
        super(in);
        /*
         * Looks like: Received: from iamhelo (wasabi.infohazard.org
         * [209.237.247.14]) by mx.google.com with SMTP id
         * 32si2669129wfa.13.2009.05.27.18.27.31; Wed, 27 May 2009 18:27:48
         * -0700 (PDT)
         */
        Preconditions.checkNotNull(heloHost);
        Preconditions.checkNotNull(softwareName);
        Preconditions.checkNotNull(singleRecipient);
        DateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z (z)", Locale.US);
        String timestamp = fmt.format(new Date());

        StringBuilder header = new StringBuilder();
        header.append("Received: from ").append(heloHost.orElse(null)).append(" (").append(constructTcpInfo(host)).append(")\r\n");
        header.append("        by ").append(whoami).append("\r\n");
        header.append("        with SMTP");
        if (softwareName.isPresent())
            header.append(" (").append(softwareName.get()).append(")");
        header.append(" id ").append(id);
        if (singleRecipient.isPresent())
            header.append("\r\n        for ").append(singleRecipient.get());
        header.append(";\r\n");
        header.append("        ").append(timestamp).append("\r\n");

        this.header = new ByteArrayInputStream(TextUtils.getAsciiBytes(header.toString()));
    }

    /**
     * Returns a formatted TCP-info element, depending on the success of the IP
     * address name resolution either with domain name or only the address
     * literal.
     * 
     * @param host
     *            the address of the remote SMTP client.
     * @return the formatted TCP-info element as defined by RFC 5321
     */
    private String constructTcpInfo(InetAddress host) {
        // if it is not successful it just returns the address
        String domain = host.getCanonicalHostName();
        String address = host.getHostAddress();
        // check whether the host name resolution was successful
        if (domain.equals(address))
            return "[" + address + "]";
        else
            return domain + " [" + address + "]";
    }

    /* */
    @Override
    public int available() throws IOException {
        return this.header.available() + super.available();
    }

    /* */
    @Override
    public void close() throws IOException {
        super.close();
    }

    /* */
    @Override
    public synchronized void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    /* */
    @Override
    public boolean markSupported() {
        return false;
    }

    /* */
    @Override
    public int read() throws IOException {
        if (this.header.available() > 0)
            return this.header.read();
        else
            return super.read();
    }

    /* */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.header.available() > 0) {
            int countRead = this.header.read(b, off, len);
            if (countRead < len) {
                // We need to add a little extra from the normal stream
                int remainder = len - countRead;
                int additionalRead = super.read(b, off + countRead, remainder);

                return countRead + additionalRead;
            } else
                return countRead;
        } else
            return super.read(b, off, len);
    }

    /* */
    @Override
    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    /* */
    @Override
    public synchronized void reset() throws IOException {
        throw new UnsupportedOperationException();
    }

    /* */
    @Override
    public long skip(long n) throws IOException {
        throw new UnsupportedOperationException();
    }
}
