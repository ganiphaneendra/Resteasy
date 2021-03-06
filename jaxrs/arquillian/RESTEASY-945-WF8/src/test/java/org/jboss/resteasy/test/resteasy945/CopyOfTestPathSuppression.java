package org.jboss.resteasy.test.resteasy945;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.jboss.resteasy.api.validation.Validation;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.resteasy945.TestApplication;
import org.jboss.resteasy.resteasy945.TestClassValidator;
import org.jboss.resteasy.resteasy945.TestClassConstraint;
import org.jboss.resteasy.resteasy945.TestResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * RESTEASY-945
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 * Copyright May 14, 2014
 */
@RunWith(Arquillian.class)
public class CopyOfTestPathSuppression
{  
   @Deployment(name="war-default")
   public static Archive<?> createTestArchive1Default()
   {
      WebArchive war = ShrinkWrap.create(WebArchive.class, "RESTEASY-945-default.war")
            .addClasses(TestApplication.class, TestResource.class)
            .addClasses(TestClassConstraint.class, TestClassValidator.class)
            .addAsWebInfResource("web-default.xml", "web.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            ;
      System.out.println(war.toString(true));
      return war;
   }
   
   @Test
   public void testInputSuppressPathDefault() throws Exception
   {
      doTestInputViolations("default", "s", "t", "", "test.arg0");
   }

   @Test
   public void testInputSuppressPathFalse() throws Exception
   {
      doTestInputViolations("false", "s", "t", "", "test.arg0");
   }
   
   @Test
   public void testInputSuppressPathTrue() throws Exception
   {
      doTestInputViolations("true", "*", "*", "*", "*");
   }
   
   //@Test
   public void testReturnValueSuppressPathDefault() throws Exception
   {
      doTestReturnValueViolations(null, "test.<return value>");
   }

   //@Test
   public void testReturnValueSuppressPathFalse() throws Exception
   {
      doTestReturnValueViolations("false", "test.<return value>");
   }
   
   //@Test
   public void testReturnSuppressPathTrue() throws Exception
   {
      doTestReturnValueViolations("true", "*");
   }
   
   public void doTestInputViolations(String suppress, String fieldPath, String propertyPath, String classPath, String parameterPath) throws Exception
   {
      ResteasyClient client = new ResteasyClientBuilder().build();
      Builder builder = client.target("http://localhost:8080/RESTEASY-945-" + suppress + "/all/a/b/c").request();
      Response response = builder.get();
      System.out.println("status: " + response.getStatus());
      Object header = response.getHeaders().getFirst(Validation.VALIDATION_HEADER);
      Assert.assertTrue(header instanceof String);
      Assert.assertTrue(Boolean.valueOf(String.class.cast(header)));
      String answer = response.readEntity(String.class);
      System.out.println("entity: " + answer);
      assertEquals(400, response.getStatus());
      ResteasyViolationException e = new ResteasyViolationException(String.class.cast(answer));
      countViolations(e, 1, 1, 1, 1, 0);
      ResteasyConstraintViolation violation = e.getFieldViolations().iterator().next();
      System.out.println("violation: " + violation);
      System.out.println("field path: " + violation.getPath());
      Assert.assertEquals(fieldPath, violation.getPath());
      violation = e.getPropertyViolations().iterator().next();
      System.out.println("property path: " + violation.getPath());
      Assert.assertEquals(propertyPath, violation.getPath());
      violation = e.getClassViolations().iterator().next();
      System.out.println("class path: " + violation.getPath());
      Assert.assertEquals(classPath, violation.getPath());;
      violation = e.getParameterViolations().iterator().next();
      System.out.println("parameter path: " + violation.getPath());
      Assert.assertEquals(parameterPath, violation.getPath());
   }
   
   public void doTestReturnValueViolations(String suppress, String returnValuePath) throws Exception
   {
      ResteasyClient client = new ResteasyClientBuilder().build();
      Builder builder = client.target("http://localhost:8080/RESTEASY-945/all/aa/bbb/cccc").request();
//      builder.accept(MediaType.APPLICATION_XML);
      Response response = builder.get();
      System.out.println("status: " + response.getStatus());
      Object header = response.getHeaders().getFirst(Validation.VALIDATION_HEADER);
      Assert.assertTrue(header instanceof String);
      Assert.assertTrue(Boolean.valueOf(String.class.cast(header)));
      String answer = response.readEntity(String.class);
      System.out.println("entity: " + answer);
      assertEquals(400, response.getStatus());
      ResteasyViolationException e = new ResteasyViolationException(String.class.cast(answer));
      countViolations(e, 0, 0, 0, 0, 1);
      ResteasyConstraintViolation violation = e.getReturnValueViolations().iterator().next();
      System.out.println("return value path: " + violation.getPath());
      Assert.assertEquals(returnValuePath, violation.getPath());
   }
   
   private void countViolations(ResteasyViolationException e, int fieldCount, int propertyCount, int classCount, int parameterCount, int returnValueCount)
   {
      Assert.assertEquals(fieldCount, e.getFieldViolations().size());
      Assert.assertEquals(propertyCount, e.getPropertyViolations().size());
      Assert.assertEquals(classCount, e.getClassViolations().size());
      Assert.assertEquals(parameterCount, e.getParameterViolations().size());
      Assert.assertEquals(returnValueCount, e.getReturnValueViolations().size());
   }
}