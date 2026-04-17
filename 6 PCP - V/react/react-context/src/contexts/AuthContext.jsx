import { createContext, useContext, useState } from "react";

//CONTEXT
export const AuthContext = createContext();

//PROVIDER - state varibales and functions that we want to share across the components
export const AuthProvider = ({ children }) => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  return (
    <>
      <AuthContext.Provider value={{ isLoggedIn, setIsLoggedIn }}>
        {children}
      </AuthContext.Provider>
    </>
  );
};

//CONSUMER - custom hook to use the context values in the components
export const useAuth = () => useContext(AuthContext);

// header login dashboard 100
