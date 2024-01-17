package cz.vodafone.profilecache.profileprovider.servlets;

import cz.vodafone.profilecache.maintenance.Status;
import cz.vodafone.profilecache.profileprovider.ejb.StatusHolderBean;
import org.apache.log4j.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = "/status.jsp")
public class StatusPageServlet extends javax.servlet.http.HttpServlet {

    private static final Logger LOG = Logger.getLogger(StatusPageServlet.class);

    @EJB
    private StatusHolderBean statusHolderBean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = null;
        try {
            resp.setStatus(HttpServletResponse.SC_OK);
            out = resp.getWriter();


            String html;
            if (this.statusHolderBean == null || this.statusHolderBean.getStatus() == null) {
                html = "<HTML><BODY>\nSTATUS OK\n\nstatus component not available\n</BODY></HTML>";
            } else {
                Status status = this.statusHolderBean.getStatus();
                html = String.format(
                        "<HTML><BODY>\nSTATUS OK\n<br/><br/>\nERRORS:\n<br/>\ncache=%d\n<br/>\ndb=%d\n<br/>\nlocation-provider=%d\n</BODY></HTML>",
                        status.getErrorCount(Status.Component.Cache),
                        status.getErrorCount(Status.Component.Database),
                        status.getErrorCount(Status.Component.LocationProvider));
            }
            out.println(html);
            if (LOG.isDebugEnabled()) {
                LOG.debug("HTML response sent back to client\n" + html);
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
