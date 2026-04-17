import React from "react";
import { Children } from "react";
import NavBar from "../components/NavBar";
import Footer from "../components/Footer";

function MainLayout(props) {
  const { children } = props;
  return (
    <>
      <NavBar />
      <main>{children}</main>
      {/* <Footer /> */}
    </>
  );
}

export default MainLayout;
